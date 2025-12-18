package com.hamiltonjewelers.ns_sf_connector.repository;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {
}
