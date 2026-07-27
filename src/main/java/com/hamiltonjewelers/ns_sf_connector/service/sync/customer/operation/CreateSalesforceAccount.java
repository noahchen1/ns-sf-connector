package com.hamiltonjewelers.ns_sf_connector.service.sync.customer.operation;

import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.customer.CustomerMapping;
import com.hamiltonjewelers.ns_sf_connector.service.sync.customer.CustomerSyncContext;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncSystem;
import org.springframework.stereotype.Component;

@Component
public class CreateSalesforceAccount implements CustomerSyncOperation {
    private final CustomerMapping mapping;
    private final SfAuthClient sfAuthClient;
    private final SfAccountClient sfAccountClient;

    public CreateSalesforceAccount(
            CustomerMapping mapping,
            SfAuthClient sfAuthClient,
            SfAccountClient sfAccountClient
    ) {
        this.mapping = mapping;
        this.sfAuthClient = sfAuthClient;
        this.sfAccountClient = sfAccountClient;
    }

    @Override
    public boolean supports(SyncRoute route) {
        return route.is(SyncSystem.NETSUITE, SyncSystem.SALESFORCE, SyncOperation.INSERT);
    }

    @Override
    public void execute(SyncJob job, CustomerSyncContext context) {
        CustomerDto customer =
                context.state().requireNetsuiteCustomer(context.netsuiteCustomerId());
        sfAccountClient.createAccount(
                sfAuthClient.fetchAccessToken(),
                mapping.salesforceCreateFields(customer)
        );
    }
}
