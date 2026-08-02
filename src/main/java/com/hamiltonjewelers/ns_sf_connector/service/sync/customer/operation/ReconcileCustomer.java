package com.hamiltonjewelers.ns_sf_connector.service.sync.customer.operation;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
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
import com.hamiltonjewelers.ns_sf_connector.service.sync.customer.conflict.CustomerConflictResolver;
import org.springframework.stereotype.Component;

@Component
public class ReconcileCustomer implements CustomerSyncOperation {
    private final CustomerConflictResolver conflictResolver;
    private final NsAuthClient nsAuthClient;
    private final NsCustomerClient nsCustomerClient;
    private final SfAuthClient sfAuthClient;
    private final SfAccountClient sfAccountClient;

    public ReconcileCustomer(
            CustomerConflictResolver conflictResolver,
            NsAuthClient nsAuthClient,
            NsCustomerClient nsCustomerClient,
            SfAuthClient sfAuthClient,
            SfAccountClient sfAccountClient
    ) {
        this.conflictResolver = conflictResolver;
        this.nsAuthClient = nsAuthClient;
        this.nsCustomerClient = nsCustomerClient;
        this.sfAuthClient = sfAuthClient;
        this.sfAccountClient = sfAccountClient;
    }

    @Override
    public boolean supports(SyncRoute route) {
        return route.is(SyncSystem.SYSTEM, SyncSystem.SYSTEM, SyncOperation.RECONCILE);
    }

    @Override
    public void execute(SyncJob job, CustomerSyncContext context) {
        int netsuiteId = context.netsuiteCustomerId();
        CustomerDto customer = context.state().requireNetsuiteCustomer(netsuiteId);
        AccountDto.AccountRecord account = context.state().requireSalesforceAccount(netsuiteId);
        CustomerPatches patches = conflictResolver.resolve(customer, account);

        if (!patches.salesforcePatch().isEmpty()) {
            sfAccountClient.updateAccount(
                    sfAuthClient.fetchAccessToken(),
                    account.id(),
                    patches.salesforcePatch()
            );
        }
        if (!patches.netsuitePatch().isEmpty()) {
            nsCustomerClient.updateCustomer(
                    nsAuthClient.fetchAccessToken(),
                    String.valueOf(netsuiteId),
                    patches.netsuitePatch()
            );
        }
    }
}
