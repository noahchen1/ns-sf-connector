package com.hamiltonjewelers.ns_sf_connector.repository;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {
    Optional<SyncJob> findBySourceSystemAndRecordTypeAndSourceRecordIdAndOperation(
            String sourceSystem,
            String recordType,
            String sourceRecordId,
            String operation
    );
}
