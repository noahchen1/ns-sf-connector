package com.hamiltonjewelers.ns_sf_connector.service.sync.item.operation;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.item.invLocation.NsItemClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.item.SfItemClient;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemState;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemSyncContext;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.service.sync.item.ItemMapping;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateSalesforceItemTests {
    @Test
    void createsSalesforceItemAndLinksNetsuiteItem() {
        ItemMapping mapping = mock(ItemMapping.class);
        NsAuthClient nsAuthClient = mock(NsAuthClient.class);
        NsItemClient nsItemClient = mock(NsItemClient.class);
        SfAuthClient sfAuthClient = mock(SfAuthClient.class);
        SfItemClient sfItemClient = mock(SfItemClient.class);
        CreateSalesforceItem operation = new CreateSalesforceItem(
                mapping,
                nsAuthClient,
                nsItemClient,
                sfAuthClient,
                sfItemClient
        );
        NsItemDto.ItemRecord item = new NsItemDto.ItemRecord(
                42, "SKU-1", "Display", "Vendor-1", null, null
        );
        Map<String, Object> createFields = Map.of("Name", "SKU-1");
        when(mapping.salesforceCreateFields(item)).thenReturn(createFields);
        when(sfAuthClient.fetchAccessToken()).thenReturn("sf-token");
        when(sfItemClient.createItem("sf-token", createFields)).thenReturn("a00123456789012AAA");
        when(nsAuthClient.fetchAccessToken()).thenReturn("ns-token");

        operation.execute(null, new ItemSyncContext(42, new ItemState(item, null)));

        verify(nsItemClient).updateItem(
                "ns-token",
                "42",
                Map.of("custitem_sfid", "a00123456789012AAA")
        );
    }
}
