package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.customer.operation.CustomerSyncOperation;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncHandler;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncJobService;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncRecordType;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncRoute;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerSyncHandler implements SyncHandler {
    private final CustomerLinkResolver linkResolver;
    private final CustomerStateLoader stateLoader;
    private final CustomerJobFreshnessChecker freshnessChecker;
    private final List<CustomerSyncOperation> operations;
    private final SyncJobService syncJobService;

    public CustomerSyncHandler(
            CustomerLinkResolver linkResolver,
            CustomerStateLoader stateLoader,
            CustomerJobFreshnessChecker freshnessChecker,
            List<CustomerSyncOperation> operations,
            SyncJobService syncJobService
    ) {
        this.linkResolver = linkResolver;
        this.stateLoader = stateLoader;
        this.freshnessChecker = freshnessChecker;
        this.operations = List.copyOf(operations);
        this.syncJobService = syncJobService;
    }

    @Override
    public boolean supports(SyncRecordType recordType) {
        return recordType == SyncRecordType.CUSTOMER;
    }

    @Override
    public void execute(SyncJob job, SyncRoute route) {
        int netsuiteId = linkResolver.resolveNetsuiteCustomerId(job, route);
        CustomerState state = stateLoader.load(netsuiteId);
        if (freshnessChecker.isStale(state, route)) {
            String salesforceId =
                    state.salesforceAccount() == null ? null : state.salesforceAccount().getId();
            syncJobService.supersedeAndEnqueueReconcile(
                    job.getId(),
                    netsuiteId,
                    salesforceId,
                    "Target changed since enqueue"
            );
            return;
        }

        CustomerSyncOperation operation = operations.stream()
                .filter(candidate -> candidate.supports(route))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Unsupported customer sync route: " + route
                ));
        operation.execute(job, new CustomerSyncContext(netsuiteId, state));
    }
}
