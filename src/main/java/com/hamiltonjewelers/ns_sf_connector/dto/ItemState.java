package com.hamiltonjewelers.ns_sf_connector.dto;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;

public record ItemState(
        NsItemDto.ItemRecord netsuiteItem,
        SfItemDto.ItemRecord salesforceItem
) {
    public NsItemDto.ItemRecord requireNetsuiteItem(int netsuiteItemId) {
        if (netsuiteItem == null) {
            throw new IllegalStateException("NetSuite Item is missing for ID " + netsuiteItemId);
        }
        return netsuiteItem;
    }

    public SfItemDto.ItemRecord requireSalesforceItem(int netsuiteItemId) {
        if (salesforceItem == null) {
            throw new IllegalStateException("Salesforce Item is missing for NetSuite ID " + netsuiteItemId);
        }
        return salesforceItem;
    }

    public SfItemDto.ItemRecord requireSalesforceItem(String salesforceItemId) {
        if (salesforceItem == null) {
            throw new IllegalStateException("Salesforce Item is missing for ID " + salesforceItemId);
        }
        return salesforceItem;
    }
}
