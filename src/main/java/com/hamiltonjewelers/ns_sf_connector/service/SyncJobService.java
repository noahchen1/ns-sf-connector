package com.hamiltonjewelers.ns_sf_connector.service;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.repository.SyncJobRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SyncJobService {
    private final SyncJobRepository repository;

    public SyncJobService(SyncJobRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SyncJob createSyncJob(SyncJob newJob) {
        Objects.requireNonNull(newJob, "newJob must not be null");

        Optional<SyncJob> existing = repository.findBySourceSystemAndRecordTypeAndSourceRecordIdAndOperation(
                newJob.getSourceSystem(),
                newJob.getRecordType(),
                newJob.getSourceRecordId(),
                newJob.getOperation()
        );

        if (existing.isPresent()) return existing.get();
        newJob.setId(UUID.randomUUID());

        try {
            return repository.save(newJob);
        } catch (DataIntegrityViolationException ex) {
            return repository.findBySourceSystemAndRecordTypeAndSourceRecordIdAndOperation(
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

        List<SyncJob> candidates = repository.findAvailableForClaim(claimableStatuses, limit);

        if (candidates.isEmpty()) return List.of();

        List<UUID> ids = candidates.stream().map(SyncJob::getId).toList();

        int updated = repository.claimByIds(ids, "PROCESSING", workerId);
        if (updated == 0) return List.of();

        List<SyncJob> claimed = repository.findAllById(ids);
        return claimed;
    }


}
