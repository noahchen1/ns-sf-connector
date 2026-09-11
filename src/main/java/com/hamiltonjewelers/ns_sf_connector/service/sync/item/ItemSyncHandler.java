package com.hamiltonjewelers.ns_sf_connector.service.sync.item;

import com.hamiltonjewelers.ns_sf_connector.dto.ItemState;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemSyncContext;
import com.hamiltonjewelers.ns_sf_connector.dto.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncRecordType;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.item.operation.ItemSyncOperation;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncHandler;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncJobService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ItemSyncHandler implements SyncHandler {
    private final ItemLinkResolver linkResolver;
    private final ItemStateLoader stateLoader;
    private final ItemJobFreshnessChecker freshnessChecker;
    private final List<ItemSyncOperation> operations;
    private final SyncJobService syncJobService;

    public ItemSyncHandler(
            ItemLinkResolver linkResolver,
            ItemStateLoader stateLoader,
            ItemJobFreshnessChecker freshnessChecker,
            List<ItemSyncOperation> operations,
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
        return recordType == SyncRecordType.ITEM;
    }

    @Override
    public void execute(SyncJob job, SyncRoute route) {
        ItemSyncOperation operation = findOperation(route);

        if (isSalesforceToNetsuiteInsert(route)) {
            ItemState state = stateLoader.loadSalesforceItem(job.getSourceRecordId());
            operation.execute(job, new ItemSyncContext(null, state));

            return;
        }

        int netsuiteId = linkResolver.resolveNetsuiteItemId(job, route);
        ItemState state = stateLoader.load(netsuiteId);

        if (freshnessChecker.isStale(state, route)) {
            String salesforceId = state.salesforceItem() == null ? null : state.salesforceItem().id();

            syncJobService.supersedeAndEnqueueReconcile(
                    job.getId(),
                    SyncRecordType.ITEM,
                    netsuiteId,
                    salesforceId,
                    "Target changed since enqueue"
            );

            return;
        }

        operation.execute(job, new ItemSyncContext(netsuiteId, state));
    }

    private ItemSyncOperation findOperation(SyncRoute route) {
        return operations.stream()
                .filter(candidate -> candidate.supports(route))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Unsupported item sync route: " + route
                ));
    }

    private boolean isSalesforceToNetsuiteInsert(SyncRoute route) {
        return route.is(
                SyncSystem.SALESFORCE,
                SyncSystem.NETSUITE,
                SyncOperation.INSERT
        );
    }
}
