package com.hamiltonjewelers.ns_sf_connector.service.sync.discovery.item;

import com.hamiltonjewelers.ns_sf_connector.dto.ItemChange;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncRecordType;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import com.hamiltonjewelers.ns_sf_connector.model.ScheduledSyncJob;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.repository.ScheduledSyncJobRepository;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncJobService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ItemSyncCoordinator {
    private static final String SYNC_TYPE = "SCHEDULED";

    private final ItemChangeScanner changeScanner;
    private final ItemJobPlanner jobPlanner;
    private final SyncJobService syncJobService;
    private final ScheduledSyncJobRepository scheduledJobRepository;

    public ItemSyncCoordinator(
            ItemChangeScanner changeScanner,
            ItemJobPlanner jobPlanner,
            SyncJobService syncJobService,
            ScheduledSyncJobRepository scheduledJobRepository
    ) {
        this.changeScanner = changeScanner;
        this.jobPlanner = jobPlanner;
        this.syncJobService = syncJobService;
        this.scheduledJobRepository = scheduledJobRepository;
    }

    public List<SyncJob> discoverAndEnqueue() {
        LocalDateTime scanStartedAt = LocalDateTime.now();
        ScheduledSyncJob schedule = findOrCreateSchedule(scanStartedAt);

        LocalDateTime since = schedule.getLastSuccessfulAt();

        List<ItemChange> changes = changeScanner.scan(since);
        List<SyncJob> jobs = syncJobService.enqueueAll(jobPlanner.plan(changes, scanStartedAt));

        schedule.setLastSuccessfulAt(scanStartedAt);
        schedule.setUpdatedAt(LocalDateTime.now());
        scheduledJobRepository.save(schedule);

        return jobs;
    }

    private ScheduledSyncJob findOrCreateSchedule(LocalDateTime now) {
        ScheduledSyncJob existing = scheduledJobRepository.getLastScheduledSyncJob(
                SyncSystem.NETSUITE.name(),
                SyncSystem.SALESFORCE.name(),
                SyncRecordType.ITEM.name(),
                SYNC_TYPE
        );

        if (existing != null) {
            return existing;
        }

        ScheduledSyncJob schedule = new ScheduledSyncJob();
        schedule.setId(UUID.randomUUID());
        schedule.setSourceSystem(SyncSystem.NETSUITE.name());
        schedule.setTargetSystem(SyncSystem.SALESFORCE.name());
        schedule.setRecordType(SyncRecordType.ITEM.name());
        schedule.setSyncType(SYNC_TYPE);
        schedule.setLastSuccessfulAt(now.minusHours(1));
        schedule.setUpdatedAt(now);

        return schedule;
    }

}
