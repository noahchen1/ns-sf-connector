package com.hamiltonjewelers.ns_sf_connector.service;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SyncJobFactory {
    private final NsAuthClient nsAuthClient;
    private final SfAuthClient sfAuthClient;
    private final NsCustomerClient nsCustomerClient;
    private final SfAccountClient sfAccountClient;

    public SyncJobFactory(
            NsAuthClient nsAuthClient,
            SfAuthClient sfAuthClient,
            NsCustomerClient nsCustomerClient,
            SfAccountClient sfAccountClient
    ) {
        this.nsAuthClient = nsAuthClient;
        this.sfAuthClient = sfAuthClient;
        this.nsCustomerClient = nsCustomerClient;
        this.sfAccountClient = sfAccountClient;
    }

    public List<SyncJob> buildCustomerSyncJobs() {
        String nsToken = nsAuthClient.fetchAccessToken();
        String sfToken = sfAuthClient.fetchAccessToken();

        LocalDateTime since = LocalDateTime.now().minusHours(1);
        LocalDateTime now = LocalDateTime.now();

        List<SyncJob> jobs = new ArrayList<>();

        List<CustomerItemDto> nsChanged = nsCustomerClient.getCustomers(nsToken, since);
        Map<Integer, CustomerItemDto> nsChangedMap = nsChanged.stream()
                .filter(c -> c.getInternalId() != null)
                .collect(Collectors.toMap(CustomerItemDto::getInternalId, c -> c, (a, b) -> a));
        Map<Integer, AccountDto.AccountRecord> sfLookupMap = sfAccountClient
                .getAccountsByNetsuiteIds(sfToken, nsChangedMap.keySet())
                .stream()
                .filter(a -> a.getNetsuiteId() != null)
                .collect(Collectors.toMap(AccountDto.AccountRecord::getNetsuiteId, a -> a, (a, b) -> a));

        for (Integer netsuiteId : nsChangedMap.keySet()) {
            SyncJob job = buildJob(
                    netsuiteId,
                    nsChangedMap.get(netsuiteId),
                    sfLookupMap.get(netsuiteId),
                    now
            );

            if (job != null) jobs.add(job);
        }

        List<AccountDto.AccountRecord> sfChanged = sfAccountClient.getAccounts(sfToken, since);
        Map<Integer, AccountDto.AccountRecord> sfChangedMap = sfChanged.stream()
                .filter(a -> a.getNetsuiteId() != null)
                .collect(Collectors.toMap(AccountDto.AccountRecord::getNetsuiteId, a -> a, (a, b) -> a));

        Map<Integer, CustomerItemDto> nsLookupMap = nsCustomerClient
                .getCustomersByInternalIds(nsToken, sfChangedMap.keySet())
                .stream()
                .filter(c -> c.getInternalId() != null)
                .collect(Collectors.toMap(CustomerItemDto::getInternalId, c -> c, (a, b) -> a));

        for (Integer netsuiteId : sfChangedMap.keySet()) {
            SyncJob job = buildJob(
                    netsuiteId,
                    nsLookupMap.get(netsuiteId),
                    sfChangedMap.get(netsuiteId),
                    now
            );
            if (job != null) jobs.add(job);
        }

        return jobs;
    }

    private SyncJob buildJob(
            Integer netsuiteId,
            CustomerItemDto nsCustomer,
            AccountDto.AccountRecord sfAccount,
            LocalDateTime now
    ) {
        System.out.printf("nsCustomer: %s", nsCustomer);
        System.out.printf("sfCustomer: %s", sfAccount);

        SyncDecision decision = decide(nsCustomer, sfAccount);

        return switch (decision) {
            case NS_TO_SF_INSERT -> newJob(
                    "Netsuite",
                    "Salesforce",
                    "Customer",
                    String.valueOf(netsuiteId),
                    null,
                    "INSERT", now
            );
            case SF_TO_NS_INSERT -> newJob(
                    "Salesforce",
                    "Netsuite",
                    "Customer",
                    sfAccount.getId(),
                    String.valueOf(netsuiteId),
                    "INSERT",
                    now
            );
            case NS_TO_SF_UPDATE -> newJob(
                    "Netsuite",
                    "Salesforce",
                    "Customer",
                    String.valueOf(netsuiteId),
                    sfAccount != null ? sfAccount.getId() : null,
                    "UPDATE", now
            );
            case SF_TO_NS_UPDATE -> newJob(
                    "Salesforce",
                    "Netsuite",
                    "Customer",
                    sfAccount.getId(),
                    String.valueOf(netsuiteId),
                    "UPDATE", now
            );
            case NO_OP -> null;
        };
    }

    private SyncDecision decide(CustomerItemDto ns, AccountDto.AccountRecord sf) {
        if (ns != null && sf == null) return SyncDecision.NS_TO_SF_INSERT;
        if (ns == null && sf != null) return SyncDecision.SF_TO_NS_INSERT;
        if (ns == null) return SyncDecision.NO_OP;

        LocalDateTime nsModified = ns.getLastModifiedDate();
        LocalDateTime sfModified = sf.getLastModifiedDate();

        if (nsModified == null && sfModified == null) return SyncDecision.NO_OP;
        if (sfModified == null) return SyncDecision.NS_TO_SF_UPDATE;
        if (nsModified == null) return SyncDecision.SF_TO_NS_UPDATE;

        int cmp = nsModified.compareTo(sfModified);

        if (cmp > 0) return SyncDecision.NS_TO_SF_UPDATE;
        if (cmp < 0) return SyncDecision.SF_TO_NS_UPDATE;

        return SyncDecision.NS_TO_SF_UPDATE;
    }

    private SyncJob newJob(
            String sourceSystem,
            String targetSystem,
            String recordType,
            String sourceRecordId,
            String targetRecordId,
            String operation,
            LocalDateTime now
    ) {
        SyncJob job = new SyncJob();

        job.setId(UUID.randomUUID());
        job.setSourceSystem(sourceSystem);
        job.setTargetSystem(targetSystem);
        job.setRecordType(recordType);
        job.setSourceRecordId(sourceRecordId);
        job.setTargetRecordId(targetRecordId);
        job.setSyncType("SCHEDULED");
        job.setOperation(operation);
        job.setPriority(5);
        job.setStatus("PENDING");
        job.setAttemptCount(0);
        job.setMaxAttempts(5);
        job.setAvailableAt(now);
        job.setClaimedAt(null);
        job.setClaimedBy(null);
        job.setErrorMessage(null);

        return job;
    }

    private enum SyncDecision {
        NS_TO_SF_INSERT,
        SF_TO_NS_INSERT,
        NS_TO_SF_UPDATE,
        SF_TO_NS_UPDATE,
        NO_OP
    }
}
