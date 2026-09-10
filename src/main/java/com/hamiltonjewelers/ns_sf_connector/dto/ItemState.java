package com.hamiltonjewelers.ns_sf_connector.dto;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;

public record ItemState(
        NsItemDto.ItemRecord netsuiteItem,
        SfItemDto.ItemRecord salesforceItem
) {
//    public NsItemDto.ItemRecord requireNetsuiteItem(int netsuiteItemId) {
//        if (netsuiteItem == null) {
//            throw new IllegalStateException("NetSuite Item is missing for ID " + netsuiteId);
//        }
//
//        return netsuiteItem;
//    }
}
