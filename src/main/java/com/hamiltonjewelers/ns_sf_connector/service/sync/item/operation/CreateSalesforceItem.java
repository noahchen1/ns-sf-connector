package com.hamiltonjewelers.ns_sf_connector.service.sync.item.operation;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.item.invLocation.NsItemClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.item.SfItemClient;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemSyncContext;
import com.hamiltonjewelers.ns_sf_connector.dto.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.item.ItemMapping;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CreateSalesforceItem implements ItemSyncOperation {
    private final ItemMapping mapping;
    private final NsAuthClient nsAuthClient;
    private final NsItemClient nsItemClient;
    private final SfAuthClient sfAuthClient;
    private final SfItemClient sfItemClient;

    public CreateSalesforceItem(
            ItemMapping mapping,
            NsAuthClient nsAuthClient,
            NsItemClient nsItemClient,
            SfAuthClient sfAuthClient,
            SfItemClient sfItemClient
    ) {
        this.mapping = mapping;
        this.nsAuthClient = nsAuthClient;
        this.nsItemClient = nsItemClient;
        this.sfAuthClient = sfAuthClient;
        this.sfItemClient = sfItemClient;
    }

    @Override
    public boolean supports(SyncRoute route) {
        return route.is(SyncSystem.NETSUITE, SyncSystem.SALESFORCE, SyncOperation.INSERT);
    }

    @Override
    public void execute(SyncJob job, ItemSyncContext context) {
        int netsuiteId = context.netsuiteItemId();
        NsItemDto.ItemRecord item = context.state().requireNetsuiteItem(netsuiteId);
        String salesforceId = sfItemClient.createItem(
                sfAuthClient.fetchAccessToken(),
                mapping.salesforceCreateFields(item)
        );
        nsItemClient.updateItem(
                nsAuthClient.fetchAccessToken(),
                String.valueOf(netsuiteId),
                Map.of("custitem_sfid", salesforceId)
        );
    }
}
