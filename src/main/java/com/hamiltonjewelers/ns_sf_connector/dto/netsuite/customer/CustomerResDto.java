package com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer;

import java.util.List;

public record CustomerResDto(
        List<LinkDto> links,
        int count,
        boolean hasMore,
        List<CustomerDto> items,
        int offset,
        int totalResults
) {
}
