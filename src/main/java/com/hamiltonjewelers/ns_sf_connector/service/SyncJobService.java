package com.hamiltonjewelers.ns_sf_connector.service;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.repository.SyncJobRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SyncJobService {
    private final SyncJobRepository repository;

    public SyncJobService(SyncJobRepository repository) {
        this.repository = repository;
    }

    public void createSyncJob(SyncJob newJob) {
        newJob.setId(UUID.randomUUID());

        repository.save(newJob);
    }
}
