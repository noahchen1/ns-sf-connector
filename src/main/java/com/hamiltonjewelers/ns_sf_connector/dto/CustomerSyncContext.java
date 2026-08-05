package com.hamiltonjewelers.ns_sf_connector.dto;

public record CustomerSyncContext(Integer netsuiteCustomerId, CustomerState state) {
}
