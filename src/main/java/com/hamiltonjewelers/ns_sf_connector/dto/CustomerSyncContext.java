package com.hamiltonjewelers.ns_sf_connector.dto;

public record CustomerSyncContext(int netsuiteCustomerId, CustomerState state) {
}
