package com.hamiltonjewelers.ns_sf_connector.scheduler;

import com.hamiltonjewelers.ns_sf_connector.model.ScheduledSyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.SyncJobService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SyncJobScheduler {
    private final SyncJobService syncJobService;

    public SyncJobScheduler(SyncJobService syncJobService) {
        this.syncJobService = syncJobService;
    }

    @Scheduled(fixedRate = 10000)
    public void scheduledSyncJobs() {
        try {
            ScheduledSyncJob job = new ScheduledSyncJob();

            job.setSourceSystem("Netsuite");
            job.setTargetSystem("Salesforce");
            job.setRecordType("Customer");
            job.setSyncType("SCHEDULED");
            job.setLastSuccessfulAt(LocalDateTime.now());

            ScheduledSyncJob scheduledJob = syncJobService.createScheduledSyncJob(job);

            System.out.printf("[%s] Scheduled job (Source: %s, Target: %s, " +
                            "Record: %s) successfully created/updated!%n",
                    scheduledJob.getId(),
                    scheduledJob.getSourceSystem(),
                    scheduledJob.getTargetSystem(),
                    scheduledJob.getRecordType()
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
