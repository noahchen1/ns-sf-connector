package com.hamiltonjewelers.ns_sf_connector.service.sync.customer.operation;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.CustomerSyncContext;
import com.hamiltonjewelers.ns_sf_connector.dto.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.customer.CustomerMapping;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class CreateNetsuiteCustomer implements CustomerSyncOperation {
    private final CustomerMapping mapping;
    private final NsAuthClient nsAuthClient;
    private final NsCustomerClient nsCustomerClient;
    private final SfAuthClient sfAuthClient;
    private final SfAccountClient sfAccountClient;

    public CreateNetsuiteCustomer(
            CustomerMapping mapping,
            NsAuthClient nsAuthClient,
            NsCustomerClient nsCustomerClient,
            SfAuthClient sfAuthClient,
            SfAccountClient sfAccountClient
    ) {
        this.mapping = mapping;
        this.nsAuthClient = nsAuthClient;
        this.nsCustomerClient = nsCustomerClient;
        this.sfAuthClient = sfAuthClient;
        this.sfAccountClient = sfAccountClient;
    }

    @Override
    public boolean supports(SyncRoute route) {
        return route.is(
                SyncSystem.SALESFORCE,
                SyncSystem.NETSUITE,
                SyncOperation.INSERT
        );
    }

    @Override
    public void execute(SyncJob job, CustomerSyncContext context) {
        String salesforceId = job.getSourceRecordId();
        AccountDto.AccountRecord account =
                context.state().requireSalesforceAccount(salesforceId);

        if (!Objects.equals(salesforceId, account.id())) {
            throw new IllegalStateException(
                    "Loaded Salesforce Account " + account.id()
                            + " does not match sync job source " + salesforceId
            );
        }

        // Another worker or process may have linked the Account after this job was queued.
        if (account.netsuiteId() != null) {
            return;
        }

        String netsuiteToken = nsAuthClient.fetchAccessToken();
        List<CustomerDto> existing = nsCustomerClient.getCustomersBySalesforceId(
                netsuiteToken,
                salesforceId
        );
        if (existing.size() > 1) {
            throw new IllegalStateException(
                    "Multiple NetSuite Customers are linked to Salesforce Account " + salesforceId
            );
        }

        int netsuiteId = existing.isEmpty()
                ? nsCustomerClient.createCustomer(
                        netsuiteToken,
                        mapping.netsuiteCreateFields(account)
                )
                : existing.getFirst().internalId();

        sfAccountClient.updateAccount(
                sfAuthClient.fetchAccessToken(),
                salesforceId,
                Map.of("Netsuite_Id__c", netsuiteId)
        );
    }
}
