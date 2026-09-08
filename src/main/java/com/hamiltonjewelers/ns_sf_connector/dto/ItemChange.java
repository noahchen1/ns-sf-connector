package com.hamiltonjewelers.ns_sf_connector.dto;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;

public record ItemChange(
        Integer netsuiteId,
        String salesforceId,
        NsItemDto.ItemRecord netsuiteItem,
        SfItemDto.ItemRecord salesforceItem
) {
}
