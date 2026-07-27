package com.hamiltonjewelers.ns_sf_connector.repository;

import com.hamiltonjewelers.ns_sf_connector.model.ScheduledSyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ScheduledSyncJobRepository extends JpaRepository<ScheduledSyncJob, UUID> {
    @Query(value = """
        SELECT * FROM scheduled_sync_jobs
        WHERE UPPER(source_system) = UPPER(:sourceSystem)
            AND UPPER(target_system) = UPPER(:targetSystem)
            AND UPPER(record_type) = UPPER(:recordType)
            AND UPPER(sync_type) = UPPER(:syncType)
        LIMIT 1
    """, nativeQuery = true)
    ScheduledSyncJob getLastScheduledSyncJob(
            @Param("sourceSystem") String sourceSystem,
            @Param("targetSystem") String targetSystem,
            @Param("recordType") String recordType,
            @Param("syncType") String syncType
    );
}
