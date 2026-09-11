package com.hamiltonjewelers.ns_sf_connector.service.sync.item.operation;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.item.invLocation.NsItemClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.item.SfItemClient;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemPatches;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemState;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemSyncContext;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;
import com.hamiltonjewelers.ns_sf_connector.service.sync.item.conflict.ItemConflictResolver;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReconcileItemTests {
    @Test
    void repairsMissingNetsuiteSalesforceIdAfterPartialCreate() {
        ItemConflictResolver conflictResolver = mock(ItemConflictResolver.class);
        NsAuthClient nsAuthClient = mock(NsAuthClient.class);
        NsItemClient nsItemClient = mock(NsItemClient.class);
        SfAuthClient sfAuthClient = mock(SfAuthClient.class);
        SfItemClient sfItemClient = mock(SfItemClient.class);
        ReconcileItem operation = new ReconcileItem(
                conflictResolver,
                nsAuthClient,
                nsItemClient,
                sfAuthClient,
                sfItemClient
        );
        NsItemDto.ItemRecord netsuite = new NsItemDto.ItemRecord(
                42, "SKU-1", "Display", "Vendor-1", null, null
        );
        SfItemDto.ItemRecord salesforce = new SfItemDto.ItemRecord(
                null, "a00123456789012AAA", "SKU-1", 42, "Display", "Vendor-1", null
        );
        when(conflictResolver.resolve(netsuite, salesforce))
                .thenReturn(new ItemPatches(Map.of(), Map.of()));
        when(nsAuthClient.fetchAccessToken()).thenReturn("ns-token");

        operation.execute(null, new ItemSyncContext(42, new ItemState(netsuite, salesforce)));

        verify(nsItemClient).updateItem(
                "ns-token",
                "42",
                Map.of("custitem_sfid", "a00123456789012AAA")
        );
    }
}
