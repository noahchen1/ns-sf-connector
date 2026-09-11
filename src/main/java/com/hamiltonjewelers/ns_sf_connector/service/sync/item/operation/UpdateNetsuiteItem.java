package com.hamiltonjewelers.ns_sf_connector.service.sync.item.operation;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.item.invLocation.NsItemClient;
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
public class UpdateNetsuiteItem implements ItemSyncOperation {
    private final ItemPatchPlanner patchPlanner;
    private final NsAuthClient nsAuthClient;
    private final NsItemClient nsItemClient;

    public UpdateNetsuiteItem(ItemPatchPlanner patchPlanner, NsAuthClient nsAuthClient, NsItemClient nsItemClient) {
        this.patchPlanner = patchPlanner;
        this.nsAuthClient = nsAuthClient;
        this.nsItemClient = nsItemClient;
    }

    @Override
    public boolean supports(SyncRoute route) {
        return route.is(SyncSystem.SALESFORCE, SyncSystem.NETSUITE, SyncOperation.UPDATE);
    }

    @Override
    public void execute(SyncJob job, ItemSyncContext context) {
        int netsuiteId = context.netsuiteItemId();
        NsItemDto.ItemRecord netsuite = context.state().requireNetsuiteItem(netsuiteId);
        SfItemDto.ItemRecord salesforce = context.state().requireSalesforceItem(netsuiteId);
        ItemPatches patches = patchPlanner.plan(netsuite, salesforce, false);
        if (!patches.netsuitePatch().isEmpty()) {
            nsItemClient.updateItem(
                    nsAuthClient.fetchAccessToken(),
                    String.valueOf(netsuiteId),
                    patches.netsuitePatch()
            );
        }
    }
}
