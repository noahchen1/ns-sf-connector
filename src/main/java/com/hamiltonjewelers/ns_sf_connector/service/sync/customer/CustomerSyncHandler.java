package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.SyncJobService;
import com.hamiltonjewelers.ns_sf_connector.service.sync.SyncHandler;
import com.hamiltonjewelers.ns_sf_connector.service.sync.SyncRoute;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CustomerSyncHandler implements SyncHandler {
    private final CustomerLinkResolver linkResolver;
    private final CustomerStateLoader stateLoader;
    private final CustomerMapping mapping;
    private final CustomerPatchPlanner patchPlanner;
    private final CustomerReconciler reconciler;
    private final NsAuthClient nsAuthClient;
    private final SfAuthClient sfAuthClient;
    private final NsCustomerClient nsCustomerClient;
    private final SfAccountClient sfAccountClient;
    private final SyncJobService syncJobService;

    public CustomerSyncHandler(CustomerLinkResolver linkResolver, CustomerStateLoader stateLoader,
                               CustomerMapping mapping, CustomerPatchPlanner patchPlanner,
                               CustomerReconciler reconciler, NsAuthClient nsAuthClient,
                               SfAuthClient sfAuthClient, NsCustomerClient nsCustomerClient,
                               SfAccountClient sfAccountClient, SyncJobService syncJobService) {
        this.linkResolver = linkResolver;
        this.stateLoader = stateLoader;
        this.mapping = mapping;
        this.patchPlanner = patchPlanner;
        this.reconciler = reconciler;
        this.nsAuthClient = nsAuthClient;
        this.sfAuthClient = sfAuthClient;
        this.nsCustomerClient = nsCustomerClient;
        this.sfAccountClient = sfAccountClient;
        this.syncJobService = syncJobService;
    }

    @Override
    public boolean supports(SyncRoute route) {
        return "CUSTOMER".equals(route.recordType());
    }

    @Override
    public void execute(SyncJob job, SyncRoute route) {
        if (isStale(job, route)) {
            syncJobService.supersedeAndEnqueueReconcileJob(job.getId(), "Target changed since enqueue",
                    job.getRecordType(), job.getSourceRecordId(), job.getTargetRecordId());
            return;
        }

        switch (route) {
            case SyncRoute(String source, String target, String recordType, String operation)
                    when "NETSUITE".equals(source) && "SALESFORCE".equals(target) && "INSERT".equals(operation) -> insertIntoSalesforce(job, route);
            case SyncRoute(String source, String target, String recordType, String operation)
                    when "NETSUITE".equals(source) && "SALESFORCE".equals(target) && "UPDATE".equals(operation) -> updateSalesforce(job, route);
            case SyncRoute(String source, String target, String recordType, String operation)
                    when "SALESFORCE".equals(source) && "NETSUITE".equals(target) && "UPDATE".equals(operation) -> updateNetsuite(job, route);
            case SyncRoute(String source, String target, String recordType, String operation)
                    when "SYSTEM".equals(source) && "SYSTEM".equals(target) && "RECONCILE".equals(operation) -> reconcile(job, route);
            default -> throw new UnsupportedOperationException("Unsupported customer sync route: " + route);
        }
    }

    private boolean isStale(SyncJob job, SyncRoute route) {
        if ("SYSTEM".equals(route.sourceSystem())) return false;
        CustomerState state = stateLoader.load(linkResolver.resolveNetsuiteCustomerId(job, route));
        LocalDateTime sourceModified = "NETSUITE".equals(route.sourceSystem())
                ? modifiedNetsuite(state) : modifiedSalesforce(state);
        LocalDateTime targetModified = "NETSUITE".equals(route.targetSystem())
                ? modifiedNetsuite(state) : modifiedSalesforce(state);
        return sourceModified != null && targetModified != null && targetModified.isAfter(sourceModified);
    }

    private void insertIntoSalesforce(SyncJob job, SyncRoute route) {
        int netsuiteId = linkResolver.resolveNetsuiteCustomerId(job, route);
        CustomerItemDto customer = nsCustomerClient.getCustomer(nsAuthClient.fetchAccessToken(), String.valueOf(netsuiteId)).getFirst();
        sfAccountClient.createAccount(sfAuthClient.fetchAccessToken(), mapping.salesforceCreateFields(customer));
    }

    private void updateSalesforce(SyncJob job, SyncRoute route) {
        int netsuiteId = linkResolver.resolveNetsuiteCustomerId(job, route);
        CustomerState state = requireLinkedState(netsuiteId);
        CustomerPatches patches = patchPlanner.plan(state.netsuiteCustomer(), state.salesforceAccount(), true);
        if (!patches.salesforcePatch().isEmpty()) {
            sfAccountClient.updateAccount(sfAuthClient.fetchAccessToken(), state.salesforceAccount().getId(), patches.salesforcePatch());
        }
    }

    private void updateNetsuite(SyncJob job, SyncRoute route) {
        int netsuiteId = linkResolver.resolveNetsuiteCustomerId(job, route);
        CustomerState state = requireLinkedState(netsuiteId);
        CustomerPatches patches = patchPlanner.plan(state.netsuiteCustomer(), state.salesforceAccount(), false);
        if (!patches.netsuitePatch().isEmpty()) {
            nsCustomerClient.updateCustomer(nsAuthClient.fetchAccessToken(), String.valueOf(netsuiteId), patches.netsuitePatch());
        }
    }

    private void reconcile(SyncJob job, SyncRoute route) {
        int netsuiteId = linkResolver.resolveNetsuiteCustomerId(job, route);
        CustomerState state = requireLinkedState(netsuiteId);
        CustomerPatches patches = reconciler.reconcile(state.netsuiteCustomer(), state.salesforceAccount());
        if (!patches.salesforcePatch().isEmpty()) {
            sfAccountClient.updateAccount(sfAuthClient.fetchAccessToken(), state.salesforceAccount().getId(), patches.salesforcePatch());
        }
        if (!patches.netsuitePatch().isEmpty()) {
            nsCustomerClient.updateCustomer(nsAuthClient.fetchAccessToken(), String.valueOf(netsuiteId), patches.netsuitePatch());
        }
    }

    private CustomerState requireLinkedState(int netsuiteId) {
        CustomerState state = stateLoader.load(netsuiteId);
        if (state.netsuiteCustomer() == null || state.salesforceAccount() == null) {
            throw new IllegalStateException("Cannot sync customer: linked Customer or Account is missing for NetSuite ID " + netsuiteId);
        }
        return state;
    }

    private LocalDateTime modifiedNetsuite(CustomerState state) {
        return state.netsuiteCustomer() == null ? null : state.netsuiteCustomer().getLastModifiedDate();
    }

    private LocalDateTime modifiedSalesforce(CustomerState state) {
        return state.salesforceAccount() == null ? null : state.salesforceAccount().getLastModifiedDate();
    }
}
