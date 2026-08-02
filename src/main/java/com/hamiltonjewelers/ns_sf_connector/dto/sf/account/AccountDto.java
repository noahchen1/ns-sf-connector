package com.hamiltonjewelers.ns_sf_connector.dto.sf.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record AccountDto(
        int totalSize,
        boolean done,
        List<AccountRecord> records
) {
    public record AccountRecord(
            Attributes attributes,
            @JsonProperty("Id") String id,
            @JsonProperty("Name") String name,
            @JsonProperty("Netsuite_Id__c") Integer netsuiteId,
            @JsonProperty("First_Name__c") String firstName,
            @JsonProperty("Last_Name__c") String lastName,
            @JsonProperty("Account_Email__c") String email,
            @JsonProperty("LastModifiedDate")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            LocalDateTime lastModifiedDate
    ) {
    }

    public record Attributes(String type, String url) {
    }
}
