package com.hamiltonjewelers.ns_sf_connector.service.sync.job;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.repository.SyncJobRepository;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncRecordType;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncStatus;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class SyncJobService {
    private static final List<String> ACTIVE_STATUSES =
            List.of(SyncStatus.PENDING.name(), SyncStatus.PROCESSING.name());

    private final SyncJobRepository syncJobRepository;

    public SyncJobService(SyncJobRepository syncJobRepository) {
        this.syncJobRepository = syncJobRepository;
    }

    @Transactional
    public SyncJob enqueue(SyncJob newJob) {
        Objects.requireNonNull(newJob, "newJob must not be null");

        Optional<SyncJob> existing = findActiveEquivalent(newJob);
        if (existing.isPresent()) {
            return existing.get();
        }

        newJob.setId(UUID.randomUUID());
        try {
            return syncJobRepository.save(newJob);
        } catch (DataIntegrityViolationException ex) {
            return findActiveEquivalent(newJob)
                    .orElseThrow(() -> new IllegalStateException("Failed to enqueue or recover SyncJob", ex));
        } catch (DataAccessException ex) {
            throw new IllegalStateException("Unable to enqueue SyncJob", ex);
        }
    }

    @Transactional
    public List<SyncJob> enqueueAll(List<SyncJob> newJobs) {
        Objects.requireNonNull(newJobs, "newJobs must not be null");
        List<SyncJob> results = new ArrayList<>(newJobs.size());
        for (SyncJob job : newJobs) {
            results.add(enqueue(Objects.requireNonNull(job, "newJobs contains null element")));
        }
        return results;
    }

    @Transactional
    public List<SyncJob> claim(int limit, String workerId) {
        Objects.requireNonNull(workerId, "workerId must not be null");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0");
        }

        List<SyncJob> candidates =
                syncJobRepository.findAvailableForClaim(List.of(SyncStatus.PENDING.name()), limit);
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = candidates.stream().map(SyncJob::getId).toList();
        int updated = syncJobRepository.claimByIds(ids, SyncStatus.PROCESSING.name(), workerId);
        return updated == 0 ? List.of() : syncJobRepository.findAllById(ids);
    }

    @Transactional
    public boolean complete(UUID jobId, String workerId) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(workerId, "workerId must not be null");
        return syncJobRepository.markCompleted(jobId, workerId) == 1;
    }

    @Transactional
    public void fail(UUID jobId, String workerId, Throwable failure) {
        Objects.requireNonNull(failure, "failure must not be null");
        SyncJob job = requireOwnedProcessingJob(jobId, workerId);
        boolean exhausted = job.getAttemptCount() >= job.getMaxAttempts();
        job.setStatus(exhausted ? SyncStatus.FAILED.name() : SyncStatus.PENDING.name());
        job.setAvailableAt(exhausted ? LocalDateTime.now() : LocalDateTime.now().plusSeconds(retryDelay(job)));
        job.setClaimedAt(null);
        job.setClaimedBy(null);
        job.setErrorMessage(failureMessage(failure));
        job.setUpdatedAt(LocalDateTime.now());
        syncJobRepository.save(job);
    }

    @Transactional
    public SyncJob supersedeAndEnqueueReconcile(
            UUID jobId,
            int netsuiteCustomerId,
            String salesforceAccountId,
            String reason
    ) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (syncJobRepository.markSuperseded(jobId, reason) != 1) {
            throw new IllegalStateException("Only a PROCESSING job can be superseded: " + jobId);
        }

        String netsuiteId = String.valueOf(netsuiteCustomerId);
        Optional<SyncJob> active = syncJobRepository
                .findFirstBySourceSystemAndRecordTypeAndSourceRecordIdAndOperationAndStatusIn(
                        SyncSystem.SYSTEM.name(),
                        SyncRecordType.CUSTOMER.name(),
                        netsuiteId,
                        SyncOperation.RECONCILE.name(),
                        ACTIVE_STATUSES
                );
        if (active.isPresent()) {
            return active.get();
        }

        SyncJob reconcile = newJob(
                SyncSystem.SYSTEM,
                SyncSystem.SYSTEM,
                SyncRecordType.CUSTOMER,
                netsuiteId,
                salesforceAccountId,
                "RECONCILE",
                SyncOperation.RECONCILE,
                10
        );
        reconcile.setErrorMessage("Queued after superseding job " + jobId);
        return syncJobRepository.save(reconcile);
    }

    public SyncJob newJob(
            SyncSystem source,
            SyncSystem target,
            SyncRecordType recordType,
            String sourceRecordId,
            String targetRecordId,
            String syncType,
            SyncOperation operation,
            int priority
    ) {
        SyncJob job = new SyncJob();
        job.setId(UUID.randomUUID());
        job.setSourceSystem(source.name());
        job.setTargetSystem(target.name());
        job.setRecordType(recordType.name());
        job.setSourceRecordId(sourceRecordId);
        job.setTargetRecordId(targetRecordId);
        job.setSyncType(syncType);
        job.setOperation(operation.name());
        job.setPriority(priority);
        job.setStatus(SyncStatus.PENDING.name());
        job.setAttemptCount(0);
        job.setMaxAttempts(5);
        job.setAvailableAt(LocalDateTime.now());
        return job;
    }

    private Optional<SyncJob> findActiveEquivalent(SyncJob job) {
        return syncJobRepository.findFirstBySourceSystemAndRecordTypeAndSourceRecordIdAndOperationAndStatusIn(
                job.getSourceSystem(),
                job.getRecordType(),
                job.getSourceRecordId(),
                job.getOperation(),
                ACTIVE_STATUSES
        );
    }

    private SyncJob requireOwnedProcessingJob(UUID jobId, String workerId) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(workerId, "workerId must not be null");
        SyncJob job = syncJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown SyncJob " + jobId));
        if (!SyncStatus.PROCESSING.name().equals(job.getStatus())
                || !workerId.equals(job.getClaimedBy())) {
            throw new IllegalStateException("Worker does not own PROCESSING job " + jobId);
        }
        return job;
    }

    private long retryDelay(SyncJob job) {
        int exponent = Math.min(Math.max(job.getAttemptCount(), 1), 6);
        return Math.min(1L << exponent, 60L);
    }

    private String failureMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 2000));
    }
}
