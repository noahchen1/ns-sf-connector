package com.hamiltonjewelers.ns_sf_connector.repository;

import com.hamiltonjewelers.ns_sf_connector.model.ScheduledSyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ScheduledSyncJobRepository extends JpaRepository<ScheduledSyncJob, UUID> {
    @Query(value = """
        SELECT * FROM scheduled_sync_jobs
        WHERE source_system = :sourceSystem
            AND target_system = :targetSystem
            AND record_type = :recordType
            AND sync_type = :syncType
        LIMIT 1
    """, nativeQuery = true)
    ScheduledSyncJob getLastScheduledSyncJob(
            @Param("sourceSystem") String sourceSystem,
            @Param("targetSystem") String targetSystem,
            @Param("recordType") String recordType,
            @Param("syncType") String syncType
    );
}
