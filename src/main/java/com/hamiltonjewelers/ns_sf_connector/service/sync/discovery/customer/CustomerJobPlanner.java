package com.hamiltonjewelers.ns_sf_connector.service.sync.discovery.customer;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.dto.CustomerChange;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncRecordType;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncStatus;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class CustomerJobPlanner {
    public List<SyncJob> plan(List<CustomerChange> changes, LocalDateTime availableAt) {
        return changes.stream()
                .map(change -> plan(change, availableAt))
                .filter(Objects::nonNull)
                .toList();
    }

    private SyncJob plan(CustomerChange change, LocalDateTime availableAt) {
        CustomerDto netsuite = change.netsuiteCustomer();
        AccountDto.AccountRecord salesforce = change.salesforceAccount();
        if (netsuite == null && salesforce != null) {
            return newJob(
                    SyncSystem.SALESFORCE,
                    SyncSystem.NETSUITE,
                    String.valueOf(change.salesforceId()),
                    null,
                    SyncOperation.INSERT,
                    availableAt
            );
        }
        if (netsuite != null && salesforce == null) {
            return newJob(
                    SyncSystem.NETSUITE,
                    SyncSystem.SALESFORCE,
                    String.valueOf(change.netsuiteId()),
                    null,
                    SyncOperation.INSERT,
                    availableAt
            );
        }
        if (netsuite == null) {
            return null;
        }
        if (netsuite.lastModifiedDate() == null && salesforce.lastModifiedDate() == null) {
            return null;
        }

        boolean netsuiteWins = wins(
                netsuite.lastModifiedDate(),
                salesforce.lastModifiedDate()
        );
        return netsuiteWins
                ? newJob(
                        SyncSystem.NETSUITE,
                        SyncSystem.SALESFORCE,
                        String.valueOf(change.netsuiteId()),
                        salesforce.id(),
                        SyncOperation.UPDATE,
                        availableAt
                )
                : newJob(
                        SyncSystem.SALESFORCE,
                        SyncSystem.NETSUITE,
                        salesforce.id(),
                        String.valueOf(change.netsuiteId()),
                        SyncOperation.UPDATE,
                        availableAt
                );
    }

    private boolean wins(LocalDateTime netsuiteModified, LocalDateTime salesforceModified) {
        return salesforceModified == null
                || (netsuiteModified != null && !salesforceModified.isAfter(netsuiteModified));
    }

    private SyncJob newJob(
            SyncSystem source,
            SyncSystem target,
            String sourceRecordId,
            String targetRecordId,
            SyncOperation operation,
            LocalDateTime availableAt
    ) {
        SyncJob job = new SyncJob();
        job.setId(UUID.randomUUID());
        job.setSourceSystem(source.name());
        job.setTargetSystem(target.name());
        job.setRecordType(SyncRecordType.CUSTOMER.name());
        job.setSourceRecordId(sourceRecordId);
        job.setTargetRecordId(targetRecordId);
        job.setSyncType("SCHEDULED");
        job.setOperation(operation.name());
        job.setPriority(5);
        job.setStatus(SyncStatus.PENDING.name());
        job.setAttemptCount(0);
        job.setMaxAttempts(5);
        job.setAvailableAt(availableAt);
        return job;
    }
}
