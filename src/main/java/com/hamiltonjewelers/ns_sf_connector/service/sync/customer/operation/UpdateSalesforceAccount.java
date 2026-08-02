package com.hamiltonjewelers.ns_sf_connector.service.sync.customer.operation;

import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.dto.CustomerPatches;
import com.hamiltonjewelers.ns_sf_connector.dto.CustomerSyncContext;
import com.hamiltonjewelers.ns_sf_connector.dto.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import com.hamiltonjewelers.ns_sf_connector.service.sync.customer.conflict.CustomerPatchPlanner;
import org.springframework.stereotype.Component;

@Component
public class UpdateSalesforceAccount implements CustomerSyncOperation {
    private final CustomerPatchPlanner patchPlanner;
    private final SfAuthClient sfAuthClient;
    private final SfAccountClient sfAccountClient;

    public UpdateSalesforceAccount(
            CustomerPatchPlanner patchPlanner,
            SfAuthClient sfAuthClient,
            SfAccountClient sfAccountClient
    ) {
        this.patchPlanner = patchPlanner;
        this.sfAuthClient = sfAuthClient;
        this.sfAccountClient = sfAccountClient;
    }

    @Override
    public boolean supports(SyncRoute route) {
        return route.is(SyncSystem.NETSUITE, SyncSystem.SALESFORCE, SyncOperation.UPDATE);
    }

    @Override
    public void execute(SyncJob job, CustomerSyncContext context) {
        int netsuiteId = context.netsuiteCustomerId();
        CustomerDto customer = context.state().requireNetsuiteCustomer(netsuiteId);
        AccountDto.AccountRecord account = context.state().requireSalesforceAccount(netsuiteId);
        CustomerPatches patches = patchPlanner.plan(customer, account, true);
        if (!patches.salesforcePatch().isEmpty()) {
            sfAccountClient.updateAccount(
                    sfAuthClient.fetchAccessToken(),
                    account.id(),
                    patches.salesforcePatch()
            );
        }
    }
}
