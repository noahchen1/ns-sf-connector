package com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerDto(
        @JsonProperty("internalid") int internalId,
        @JsonProperty("custid") String custId,
        @JsonProperty("lastmodifieddate")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastModifiedDate,
        String email,
        String firstname,
        String lastname,
        String address,
        String sfid,
        Integer subsidiary,
        List<LinkDto> links
) {
}
