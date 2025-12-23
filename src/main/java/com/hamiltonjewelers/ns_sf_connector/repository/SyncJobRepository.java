package com.hamiltonjewelers.ns_sf_connector.repository;

import com.hamiltonjewelers.ns_sf_connector.model.ScheduledSyncJob;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {
    Optional<SyncJob> findBySourceSystemAndRecordTypeAndSourceRecordIdAndOperation(
            String sourceSystem,
            String recordType,
            String sourceRecordId,
            String operation
    );

    @Query(value = "SELECT * FROM sync_job "
            + "WHERE status IN (:statuses) AND available_at <= now() "
            + "ORDER BY priority DESC, available_at ASC "
            + "FOR UPDATE SKIP LOCKED "
            + "LIMIT :limit",
            nativeQuery = true)
    List<SyncJob> findAvailableForClaim(@Param("statuses") List<String> statuses, @Param("limit") int limit);

    @Modifying
    @Query(value = "UPDATE sync_job SET status = :status, claimed_at = now(), claimed_by = :workerId, "
            + "attempt_count = attempt_count + 1, updated_at = now() "
            + "WHERE id IN (:ids)",
            nativeQuery = true)
    int claimByIds(@Param("ids") List<UUID> ids, @Param("status") String status, @Param("workerId") String workerId);

    @Query(value = """
        SELECT target_system, record_type, operation, COUNT(*) AS count, MIN(available_at) AS available_at
        FROM sync_job
        WHERE status IN (:statuses) AND available_at <= CURRENT_TIMESTAMP
        GROUP BY target_system, record_type, operation
        ORDER BY COUNT(*) DESC, MIN(available_at) ASC
        LIMIT 1
    """, nativeQuery = true)
    List<Object[]> findLargestGroup(@Param("statuses") List<String> statuses);

    @Query(value = """
        SELECT * FROM sync_job
        WHERE target_system = :targetSystem
          AND record_type = :recordType
          AND operation = :operation
          AND status IN (:statuses)
          AND available_at <= CURRENT_TIMESTAMP
        ORDER BY priority DESC, available_at ASC
        FOR UPDATE SKIP LOCKED
        LIMIT :limit
    """, nativeQuery = true)
    List<SyncJob> findJobsForGroup(
            @Param("statuses") List<String> statuses,
            @Param("targetSystem") String targetSystem,
            @Param("recordType") String recordType,
            @Param("operation") String operation,
            @Param("limit") int limit);


}
