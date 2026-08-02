package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

import com.hamiltonjewelers.ns_sf_connector.dto.CustomerState;
import com.hamiltonjewelers.ns_sf_connector.dto.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CustomerJobFreshnessChecker {
    public boolean isStale(CustomerState state, SyncRoute route) {
        if (route.sourceSystem() == SyncSystem.SYSTEM) {
            return false;
        }
        if (route.operation() == SyncOperation.INSERT) {
            return route.targetSystem() == SyncSystem.SALESFORCE
                    ? state.salesforceAccount() != null
                    : state.netsuiteCustomer() != null;
        }
        LocalDateTime sourceModified = modified(state, route.sourceSystem());
        LocalDateTime targetModified = modified(state, route.targetSystem());
        return sourceModified != null && targetModified != null && targetModified.isAfter(sourceModified);
    }

    private LocalDateTime modified(CustomerState state, SyncSystem system) {
        return switch (system) {
            case NETSUITE -> state.netsuiteCustomer() == null
                    ? null
                    : state.netsuiteCustomer().getLastModifiedDate();
            case SALESFORCE -> state.salesforceAccount() == null
                    ? null
                    : state.salesforceAccount().getLastModifiedDate();
            case SYSTEM -> null;
        };
    }
}
