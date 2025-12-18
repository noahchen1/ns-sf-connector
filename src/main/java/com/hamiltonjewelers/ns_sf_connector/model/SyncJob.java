package com.hamiltonjewelers.ns_sf_connector.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "sync_job")
public class SyncJob {
    @Id
    private UUID id;

    @Column(name = "source_system", nullable = false, length = 20)
    private String sourceSystem;

    @Column(name = "target_system", nullable = false, length = 20)
    private String targetSystem;

    @Column(name = "record_type", nullable = false, length = 50)
    private String recordType;

    @Column(name = "source_record_id", nullable = false, length = 100)
    private String sourceRecordId;

    @Column(name = "target_record_id", nullable = false, length = 100)
    private String targetRecordId;

    @Column(name = "sync_type", nullable = false, length = 20)
    private String syncType;

    @Column(name = "operation", nullable = false, length = 20)
    private String operation;

    @Column(name = "grouping_key", length = 150)
    private String groupingKey;

    @Column(name = "priority")
    private Integer priority = 5;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "max_attempts")
    private Integer maxAttempts = 5;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt = LocalDateTime.now();

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters and setters for all fields

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String targetSystem) { this.targetSystem = targetSystem; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getSourceRecordId() { return sourceRecordId; }
    public void setSourceRecordId(String sourceRecordId) { this.sourceRecordId = sourceRecordId; }

    public String getTargetRecordId() { return targetRecordId; }
    public void setTargetRecordId(String targetRecordId) { this.targetRecordId = targetRecordId; }

    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getGroupingKey() { return groupingKey; }
    public void setGroupingKey(String groupingKey) { this.groupingKey = groupingKey; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }

    public LocalDateTime getAvailableAt() { return availableAt; }
    public void setAvailableAt(LocalDateTime availableAt) { this.availableAt = availableAt; }

    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }

    public String getClaimedBy() { return claimedBy; }
    public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
