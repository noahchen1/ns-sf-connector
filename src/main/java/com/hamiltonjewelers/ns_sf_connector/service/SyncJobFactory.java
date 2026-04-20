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
import java.util.stream.Stream;

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

        List<CustomerItemDto> customers = nsCustomerClient.getCustomers(nsToken, since);
        List<AccountDto.AccountRecord> accounts = sfAccountClient.getAccounts(sfToken, since);

        Map<Integer, CustomerItemDto> nsMap = customers.stream()
                .filter(c -> c.getInternalId() != null)
                .collect(Collectors.toMap(CustomerItemDto::getInternalId, c -> c));

        Map<Integer, AccountDto.AccountRecord> sfMap = accounts.stream()
                .collect(Collectors.toMap(AccountDto.AccountRecord::getNetsuiteId, c -> c));

        Set<Integer> allKeys = Stream.concat(nsMap.keySet().stream(), sfMap.keySet().stream()).collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();

        return allKeys.stream()
                .map(key -> buildJob(key, nsMap.get(key), sfMap.get(key), now))
                .filter(Objects::nonNull)
                .toList();
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
