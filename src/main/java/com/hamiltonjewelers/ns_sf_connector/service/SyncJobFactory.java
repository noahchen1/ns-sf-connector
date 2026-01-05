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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SyncJobFactory {
    private final NsAuthClient nsAuthClient;
    private final SfAuthClient sfAuthClient;
    private final NsCustomerClient nsCustomerClient;
    private final SfAccountClient sfAccountClient;

    public SyncJobFactory(NsAuthClient nsAuthClient, SfAuthClient sfAuthClient, NsCustomerClient nsCustomerClient, SfAccountClient sfAccountClient) {
        this.nsAuthClient = nsAuthClient;
        this.sfAuthClient = sfAuthClient;
        this.nsCustomerClient = nsCustomerClient;
        this.sfAccountClient = sfAccountClient;
    }

    public List<SyncJob> buildCustomerInsertJobs() {
        String nsToken = nsAuthClient.fetchAccessToken();
        String sfToken = sfAuthClient.fetchAccessToken();

        List<CustomerItemDto> customers = nsCustomerClient.getCustomers(nsToken);
        List<AccountDto.AccountRecord> accounts = sfAccountClient.getAccounts(sfToken);

        Map<Integer, CustomerItemDto> nsMap = customers.stream()
                .collect(Collectors.toMap(CustomerItemDto::getInternalId, c -> c));

        Map<Integer, AccountDto.AccountRecord> sfMap = accounts.stream()
                .collect(Collectors.toMap(AccountDto.AccountRecord::getNetsuiteId, c -> c));

        return nsMap.keySet().stream()
                .filter(netsuiteId -> !sfMap.containsKey(netsuiteId))
                .map(nsMap::get)
                .map(customer -> {
                    String internalId = String.valueOf(customer.getInternalId());

                    SyncJob job = new SyncJob();
                    job.setId(UUID.randomUUID());
                    job.setSourceSystem("Netsuite");
                    job.setTargetSystem("Salesforce");
                    job.setRecordType("Customer");
                    job.setSourceRecordId(internalId);
                    job.setTargetRecordId(null);
                    job.setSyncType("SCHEDULED");
                    job.setOperation("INSERT");
                    job.setPriority(5);
                    job.setStatus("PENDING");
                    job.setAttemptCount(0);
                    job.setMaxAttempts(5);
                    job.setAvailableAt(LocalDateTime.now());
                    job.setClaimedAt(null);
                    job.setClaimedBy(null);
                    job.setErrorMessage(null);

                    return job;
                }).collect(Collectors.toList());
    }
}
