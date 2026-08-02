package com.hamiltonjewelers.ns_sf_connector.dto.netsuite.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NsAuthResponseDto(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") int expiresIn
) {
}
