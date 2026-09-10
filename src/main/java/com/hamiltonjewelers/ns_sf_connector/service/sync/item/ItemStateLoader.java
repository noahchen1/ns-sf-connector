package com.hamiltonjewelers.ns_sf_connector.service.sync.item;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.item.invLocation.NsItemClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.item.SfItemClient;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemState;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ItemStateLoader {
    private final NsAuthClient nsAuthClient;
    private final SfAuthClient sfAuthClient;
    private final NsItemClient nsItemClient;
    private final SfItemClient sfItemClient;

    public ItemStateLoader(
            NsAuthClient nsAuthClient,
            SfAuthClient sfAuthClient,
            NsItemClient nsItemClient,
            SfItemClient sfItemClient
    ) {
        this.nsAuthClient = nsAuthClient;
        this.sfAuthClient = sfAuthClient;
        this.nsItemClient = nsItemClient;
        this.sfItemClient = sfItemClient;
    }

    public ItemState load(int netsuiteId) {
        List<NsItemDto.ItemRecord> netsuiteRows = nsItemClient.getItem(
                nsAuthClient.fetchAccessToken(),
                String.valueOf(netsuiteId)
        );

        List<SfItemDto.ItemRecord> salesforceRows = sfItemClient.getItemsByNetsuiteIds(
                sfAuthClient.fetchAccessToken(),
                Set.of(netsuiteId)
        );

        return new ItemState(firstOrNull(netsuiteRows), firstOrNull(salesforceRows));
    }

    public ItemState loadSalesforceItem(String salesforceId) {
        SfItemDto.ItemRecord item = sfItemClient.getItemById(
            sfAuthClient.fetchAccessToken(),
            salesforceId
        );

        return new ItemState(null, item);
    }

    private <T> T firstOrNull(List<T> rows) {
        return rows == null || rows.isEmpty() ? null : rows.getFirst();
    }
}
