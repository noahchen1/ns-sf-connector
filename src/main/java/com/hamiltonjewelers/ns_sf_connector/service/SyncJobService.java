package com.hamiltonjewelers.ns_sf_connector.service;
import com.hamiltonjewelers.ns_sf_connector.model.ScheduledSyncJob;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.repository.ScheduledSyncJobRepository;
import com.hamiltonjewelers.ns_sf_connector.repository.SyncJobRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SyncJobService {
    private final SyncJobRepository syncJobRepository;
    private final ScheduledSyncJobRepository scheduledSyncJobRepository;
    private final SyncJobFactory syncJobFactory;

    public SyncJobService(SyncJobRepository syncJobRepository,
                          ScheduledSyncJobRepository scheduledSyncJobRepository,
                          SyncJobFactory syncJobFactory) {
        this.syncJobRepository = syncJobRepository;
        this.scheduledSyncJobRepository = scheduledSyncJobRepository;
        this.syncJobFactory = syncJobFactory;
    }

    @Transactional
    public SyncJob createSyncJob(SyncJob newJob) {
        Objects.requireNonNull(newJob, "newJob must not be null");

        Optional<SyncJob> existing = syncJobRepository.findBySourceSystemAndRecordTypeAndSourceRecordIdAndOperation(
                newJob.getSourceSystem(),
                newJob.getRecordType(),
                newJob.getSourceRecordId(),
                newJob.getOperation()
        );

        if (existing.isPresent()) return existing.get();
        newJob.setId(UUID.randomUUID());

        try {
            return syncJobRepository.save(newJob);
        } catch (DataIntegrityViolationException ex) {
            return syncJobRepository.findBySourceSystemAndRecordTypeAndSourceRecordIdAndOperation(
                    newJob.getSourceSystem(),
                    newJob.getRecordType(),
                    newJob.getSourceRecordId(),
                    newJob.getOperation()
            ).orElseThrow(() -> new RuntimeException("Failed to create or recover SyncJob", ex));
        } catch (DataAccessException ex) {
            throw new RuntimeException("Unable to create SyncJob", ex);
        }
    }

    @Transactional
    public List<SyncJob> createSyncJobs(List<SyncJob> newJobs) {
        Objects.requireNonNull(newJobs, "newJobs must not be null");
        if (newJobs.isEmpty()) return List.of();

        List<SyncJob> results = new ArrayList<>(newJobs.size());

        for (SyncJob newJob : newJobs) {
            if (newJob == null) throw new IllegalArgumentException("newJobs contains null element");

            try {
                results.add(createSyncJob(newJob));
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create SyncJob", ex);
            }
        }

        return results;
    }

    @Transactional
    public List<SyncJob> claimJobs(int limit, String workerId) {
        Objects.requireNonNull(workerId, "workerId must not be null");
        if (limit <= 0) throw new IllegalArgumentException("limit must be greater than 0");

        List<String> claimableStatuses = List.of("PENDING");

        List<SyncJob> candidates = syncJobRepository.findAvailableForClaim(claimableStatuses, limit);

        if (candidates.isEmpty()) return List.of();

        List<UUID> ids = candidates.stream().map(SyncJob::getId).toList();

        int updated = syncJobRepository.claimByIds(ids, "PROCESSING", workerId);
        if (updated == 0) return List.of();

        return syncJobRepository.findAllById(ids);
    }

    /**
     * A superseded job must have been claimed by a worker. Failing fast here
     * prevents a stale worker from enqueueing a reconcile after another worker
     * has already changed the job state.
     */
    @Transactional
    public void markSuperseded(UUID jobId, String reason) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");

        if (syncJobRepository.markSuperseded(jobId, reason) != 1) {
            throw new IllegalStateException("Only a PROCESSING job can be superseded: " + jobId);
        }
    }

    /**
     * Coalesce concurrent stale-job detections into one active reconcile job.
     * A completed reconcile is deliberately not reused: a later conflict must
     * be reconciled again.
     */
    @Transactional
    public SyncJob enqueueReconcileJob(String recordType, String sourceRecordId,
                                       String targetRecordId, UUID supersededJobId) {
        Objects.requireNonNull(recordType, "recordType must not be null");
        Objects.requireNonNull(sourceRecordId, "sourceRecordId must not be null");
        Objects.requireNonNull(supersededJobId, "supersededJobId must not be null");

        Optional<SyncJob> activeJob = syncJobRepository
                .findFirstBySourceSystemAndRecordTypeAndSourceRecordIdAndOperationAndStatusIn(
                        "SYSTEM", recordType, sourceRecordId, "RECONCILE", List.of("PENDING", "PROCESSING"));
        if (activeJob.isPresent()) return activeJob.get();

        SyncJob job = new SyncJob();
        job.setId(UUID.randomUUID());
        job.setSourceSystem("SYSTEM");
        job.setTargetSystem("SYSTEM");
        job.setRecordType(recordType);
        job.setSourceRecordId(sourceRecordId);
        job.setTargetRecordId(targetRecordId);
        job.setSyncType("RECONCILE");
        job.setOperation("RECONCILE");
        job.setPriority(10);
        job.setStatus("PENDING");
        job.setAttemptCount(0);
        job.setMaxAttempts(5);
        job.setAvailableAt(LocalDateTime.now());
        job.setErrorMessage("Queued after superseding job " + supersededJobId);
        return syncJobRepository.save(job);
    }

    /** Keeps superseding the stale work and enqueuing its replacement atomic. */
    @Transactional
    public SyncJob supersedeAndEnqueueReconcileJob(UUID jobId, String reason, String recordType,
                                                    String sourceRecordId, String targetRecordId) {
        markSuperseded(jobId, reason);
        return enqueueReconcileJob(recordType, sourceRecordId, targetRecordId, jobId);
    }

    @Transactional
    public ScheduledSyncJob createScheduledSyncJob(ScheduledSyncJob newJob) {
        Objects.requireNonNull(newJob, "new scheduled job must not be null");

        ScheduledSyncJob existing = scheduledSyncJobRepository.getLastScheduledSyncJob(
                newJob.getSourceSystem(),
                newJob.getTargetSystem(),
                newJob.getRecordType(),
                "SCHEDULED"
        );

        if (existing != null) {
            existing.setLastSuccessfulAt(LocalDateTime.now());

            List<SyncJob> toInsertIntoSf = syncJobFactory.buildCustomerSyncJobs();
            List<SyncJob> results = createSyncJobs(toInsertIntoSf);

            System.out.printf("Scheduled sync job results: %s!%n", results);

            return scheduledSyncJobRepository.save(existing);
        } else {
            List<SyncJob> toInsertIntoSf = syncJobFactory.buildCustomerSyncJobs();
            List<SyncJob> results = createSyncJobs(toInsertIntoSf);

            System.out.printf("New Scheduled sync job results: %s!%n",
                    results);
            newJob.setId(UUID.randomUUID());
            newJob.setLastSuccessfulAt(LocalDateTime.now());
            return scheduledSyncJobRepository.save(newJob);
        }
    }
}
