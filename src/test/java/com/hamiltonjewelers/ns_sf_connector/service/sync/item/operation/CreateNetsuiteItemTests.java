package com.hamiltonjewelers.ns_sf_connector.service.sync.item.operation;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.item.invLocation.NsItemClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.item.SfItemClient;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemState;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemSyncContext;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.item.ItemMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateNetsuiteItemTests {
    private static final String SALESFORCE_ID = "a00123456789012AAA";

    private ItemMapping mapping;
    private NsAuthClient nsAuthClient;
    private NsItemClient nsItemClient;
    private SfAuthClient sfAuthClient;
    private SfItemClient sfItemClient;
    private CreateNetsuiteItem operation;

    @BeforeEach
    void setUp() {
        mapping = mock(ItemMapping.class);
        nsAuthClient = mock(NsAuthClient.class);
        nsItemClient = mock(NsItemClient.class);
        sfAuthClient = mock(SfAuthClient.class);
        sfItemClient = mock(SfItemClient.class);
        operation = new CreateNetsuiteItem(
                mapping,
                nsAuthClient,
                nsItemClient,
                sfAuthClient,
                sfItemClient
        );
    }

    @Test
    void createsNetsuiteItemAndLinksSalesforceItem() {
        SfItemDto.ItemRecord item = salesforceItem(null);
        Map<String, Object> fields = Map.of("itemId", "SKU-1");
        when(nsAuthClient.fetchAccessToken()).thenReturn("ns-token");
        when(nsItemClient.getItemsBySalesforceId("ns-token", SALESFORCE_ID)).thenReturn(List.of());
        when(mapping.netsuiteCreateFields(item)).thenReturn(fields);
        when(nsItemClient.createItem("ns-token", fields)).thenReturn(42);
        when(sfAuthClient.fetchAccessToken()).thenReturn("sf-token");

        operation.execute(job(), context(item));

        verify(nsItemClient).createItem("ns-token", fields);
        verify(sfItemClient).updateItem(
                "sf-token",
                SALESFORCE_ID,
                Map.of("Netsuite_Id__c", 42)
        );
    }

    @Test
    void reusesItemCreatedByAnEarlierAttempt() {
        SfItemDto.ItemRecord item = salesforceItem(null);
        NsItemDto.ItemRecord existing = new NsItemDto.ItemRecord(
                42, "SKU-1", null, null, SALESFORCE_ID, null
        );
        when(nsAuthClient.fetchAccessToken()).thenReturn("ns-token");
        when(nsItemClient.getItemsBySalesforceId("ns-token", SALESFORCE_ID))
                .thenReturn(List.of(existing));
        when(sfAuthClient.fetchAccessToken()).thenReturn("sf-token");

        operation.execute(job(), context(item));

        verify(nsItemClient, never()).createItem(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap()
        );
        verify(sfItemClient).updateItem(
                "sf-token",
                SALESFORCE_ID,
                Map.of("Netsuite_Id__c", 42)
        );
    }

    @Test
    void doesNothingWhenSalesforceItemIsAlreadyLinked() {
        operation.execute(job(), context(salesforceItem(42)));

        verify(nsAuthClient, never()).fetchAccessToken();
        verify(nsItemClient, never()).getItemsBySalesforceId(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private SyncJob job() {
        SyncJob job = new SyncJob();
        job.setSourceRecordId(SALESFORCE_ID);
        return job;
    }

    private ItemSyncContext context(SfItemDto.ItemRecord item) {
        return new ItemSyncContext(null, new ItemState(null, item));
    }

    private SfItemDto.ItemRecord salesforceItem(Integer netsuiteId) {
        return new SfItemDto.ItemRecord(
                null,
                SALESFORCE_ID,
                "SKU-1",
                netsuiteId,
                "Display",
                "Vendor-1",
                null
        );
    }
}
