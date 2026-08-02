package com.hamiltonjewelers.ns_sf_connector.dto.netsuite.invLocation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NsInvLocationResponseDto(
        Integer count,
        boolean hasMore,
        @JsonProperty("items") List<InvLocation> invLocations,
        int offset,
        int totalResults
) {
    public record InvLocation(
            Long item,
            Integer location,
            @JsonProperty("onhandqty") Integer quantityOnHand
    ) {
    }
}
