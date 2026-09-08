package com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record NsItemDto(
    int count,
    boolean hasMore,
    List<ItemRecord> items,
    int offset,
    int totalResults
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemRecord (
            @JsonProperty("internalid")
            int internalId,

            @JsonProperty("itemid")
            String name,

            @JsonProperty("displayname")
            String displayName,

            @JsonProperty("vendorname")
            String vendorNum,

            @JsonProperty("sfid")
            String salesforceId,

            @JsonProperty("lastmodifieddate")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime lastModifiedDate
    ) {}
}
