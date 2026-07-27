package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

public record CustomerSyncContext(int netsuiteCustomerId, CustomerState state) {
}
