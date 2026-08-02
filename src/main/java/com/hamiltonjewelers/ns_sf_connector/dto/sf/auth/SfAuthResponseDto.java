package com.hamiltonjewelers.ns_sf_connector.dto.sf.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SfAuthResponseDto(
        @JsonProperty("access_token") String accessToken,
        String signature,
        String scope,
        @JsonProperty("instance_url") String instanceUrl,
        String id,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("issued_at") String issuedAt
) {
}
