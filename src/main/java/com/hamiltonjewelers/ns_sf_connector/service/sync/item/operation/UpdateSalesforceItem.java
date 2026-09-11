package com.hamiltonjewelers.ns_sf_connector.service.sync.item.operation;

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
import com.hamiltonjewelers.ns_sf_connector.service.sync.item.conflict.ItemPatchPlanner;
import org.springframework.stereotype.Component;

@Component
public class UpdateSalesforceItem implements ItemSyncOperation {
    private final ItemPatchPlanner patchPlanner;
    private final SfAuthClient sfAuthClient;
    private final SfItemClient sfItemClient;

    public UpdateSalesforceItem(ItemPatchPlanner patchPlanner, SfAuthClient sfAuthClient, SfItemClient sfItemClient) {
        this.patchPlanner = patchPlanner;
        this.sfAuthClient = sfAuthClient;
        this.sfItemClient = sfItemClient;
    }

    @Override
    public boolean supports(SyncRoute route) {
        return route.is(SyncSystem.NETSUITE, SyncSystem.SALESFORCE, SyncOperation.UPDATE);
    }

    @Override
    public void execute(SyncJob job, ItemSyncContext context) {
        int netsuiteId = context.netsuiteItemId();
        NsItemDto.ItemRecord netsuite = context.state().requireNetsuiteItem(netsuiteId);
        SfItemDto.ItemRecord salesforce = context.state().requireSalesforceItem(netsuiteId);
        ItemPatches patches = patchPlanner.plan(netsuite, salesforce, true);
        if (!patches.salesforcePatch().isEmpty()) {
            sfItemClient.updateItem(sfAuthClient.fetchAccessToken(), salesforce.id(), patches.salesforcePatch());
        }
    }
}
