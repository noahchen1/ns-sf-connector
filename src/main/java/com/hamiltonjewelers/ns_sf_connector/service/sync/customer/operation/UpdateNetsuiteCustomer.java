package com.hamiltonjewelers.ns_sf_connector.service.sync.customer.operation;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.customer.CustomerSyncContext;
import com.hamiltonjewelers.ns_sf_connector.service.sync.customer.conflict.CustomerPatchPlanner;
import com.hamiltonjewelers.ns_sf_connector.service.sync.customer.conflict.CustomerPatches;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncSystem;
import org.springframework.stereotype.Component;

@Component
public class UpdateNetsuiteCustomer implements CustomerSyncOperation {
    private final CustomerPatchPlanner patchPlanner;
    private final NsAuthClient nsAuthClient;
    private final NsCustomerClient nsCustomerClient;

    public UpdateNetsuiteCustomer(
            CustomerPatchPlanner patchPlanner,
            NsAuthClient nsAuthClient,
            NsCustomerClient nsCustomerClient
    ) {
        this.patchPlanner = patchPlanner;
        this.nsAuthClient = nsAuthClient;
        this.nsCustomerClient = nsCustomerClient;
    }

    @Override
    public boolean supports(SyncRoute route) {
        return route.is(SyncSystem.SALESFORCE, SyncSystem.NETSUITE, SyncOperation.UPDATE);
    }

    @Override
    public void execute(SyncJob job, CustomerSyncContext context) {
        int netsuiteId = context.netsuiteCustomerId();
        CustomerDto customer = context.state().requireNetsuiteCustomer(netsuiteId);
        AccountDto.AccountRecord account = context.state().requireSalesforceAccount(netsuiteId);
        CustomerPatches patches = patchPlanner.plan(customer, account, false);
        if (!patches.netsuitePatch().isEmpty()) {
            nsCustomerClient.updateCustomer(
                    nsAuthClient.fetchAccessToken(),
                    String.valueOf(netsuiteId),
                    patches.netsuitePatch()
            );
        }
    }
}
