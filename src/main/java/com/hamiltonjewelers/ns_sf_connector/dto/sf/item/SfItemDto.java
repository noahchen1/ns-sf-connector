package com.hamiltonjewelers.ns_sf_connector.dto.sf.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record SfItemDto(
        int totalSize,
        boolean done,
        List<ItemRecord> records
) {
    public record ItemRecord(
            Attributes attributes,
            @JsonProperty("Id") String id,
            @JsonProperty("Name") String name,
            @JsonProperty("Netsuite_Id__c") Integer netsuiteId,
            @JsonProperty("Display_Name__c") String displayName,
            @JsonProperty("Vendor_Item_Number__c") String vendorNum,
            @JsonProperty("LastModifiedDate")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            LocalDateTime lastModifiedDate
    ) {}

    public record Attributes(String type, String url) {}

    public record CreateResult(
            String id,
            boolean success,
            List<Object> errors
    ) {}
}
