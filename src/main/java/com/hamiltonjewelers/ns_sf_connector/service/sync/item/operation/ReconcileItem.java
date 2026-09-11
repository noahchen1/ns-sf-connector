package com.hamiltonjewelers.ns_sf_connector.service.sync.item.operation;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.item.invLocation.NsItemClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.item.SfItemClient;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemPatches;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemSyncContext;
import com.hamiltonjewelers.ns_sf_connector.dto.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.item.conflict.ItemConflictResolver;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class ReconcileItem implements ItemSyncOperation {
    private final ItemConflictResolver conflictResolver;
    private final NsAuthClient nsAuthClient;
    private final NsItemClient nsItemClient;
    private final SfAuthClient sfAuthClient;
    private final SfItemClient sfItemClient;

    public ReconcileItem(
            ItemConflictResolver conflictResolver,
            NsAuthClient nsAuthClient,
            NsItemClient nsItemClient,
            SfAuthClient sfAuthClient,
            SfItemClient sfItemClient
    ) {
        this.conflictResolver = conflictResolver;
        this.nsAuthClient = nsAuthClient;
        this.nsItemClient = nsItemClient;
        this.sfAuthClient = sfAuthClient;
        this.sfItemClient = sfItemClient;
    }

    @Override
    public boolean supports(SyncRoute route) {
        return route.is(SyncSystem.SYSTEM, SyncSystem.SYSTEM, SyncOperation.RECONCILE);
    }

    @Override
    public void execute(SyncJob job, ItemSyncContext context) {
        int netsuiteId = context.netsuiteItemId();
        NsItemDto.ItemRecord netsuite = context.state().requireNetsuiteItem(netsuiteId);
        SfItemDto.ItemRecord salesforce = context.state().requireSalesforceItem(netsuiteId);
        ItemPatches patches = conflictResolver.resolve(netsuite, salesforce);

        repairCrossSystemLinks(netsuiteId, netsuite, salesforce);

        if (!patches.salesforcePatch().isEmpty()) {
            sfItemClient.updateItem(sfAuthClient.fetchAccessToken(), salesforce.id(), patches.salesforcePatch());
        }
        if (!patches.netsuitePatch().isEmpty()) {
            nsItemClient.updateItem(
                    nsAuthClient.fetchAccessToken(),
                    String.valueOf(netsuiteId),
                    patches.netsuitePatch()
            );
        }
    }

    private void repairCrossSystemLinks(
            int netsuiteId,
            NsItemDto.ItemRecord netsuite,
            SfItemDto.ItemRecord salesforce
    ) {
        if (netsuite.salesforceId() == null || netsuite.salesforceId().isBlank()) {
            nsItemClient.updateItem(
                    nsAuthClient.fetchAccessToken(),
                    String.valueOf(netsuiteId),
                    Map.of("custitem_sfid", salesforce.id())
            );
        } else if (!Objects.equals(netsuite.salesforceId(), salesforce.id())) {
            throw new IllegalStateException(
                    "NetSuite Item " + netsuiteId + " links to Salesforce Item "
                            + netsuite.salesforceId() + " instead of " + salesforce.id()
            );
        }

        if (salesforce.netsuiteId() == null) {
            sfItemClient.updateItem(
                    sfAuthClient.fetchAccessToken(),
                    salesforce.id(),
                    Map.of("Netsuite_Id__c", netsuiteId)
            );
        } else if (salesforce.netsuiteId() != netsuiteId) {
            throw new IllegalStateException(
                    "Salesforce Item " + salesforce.id() + " links to NetSuite Item "
                            + salesforce.netsuiteId() + " instead of " + netsuiteId
            );
        }
    }
}
