package com.hamiltonjewelers.ns_sf_connector.service.sync.item;

import com.hamiltonjewelers.ns_sf_connector.dto.ItemState;
import com.hamiltonjewelers.ns_sf_connector.dto.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ItemJobFreshnessChecker {
    public boolean isStale(ItemState state, SyncRoute route) {
        if (route.sourceSystem() == SyncSystem.SYSTEM) {
            return false;
        }

        if (route.operation() == SyncOperation.INSERT) {
            return route.targetSystem() == SyncSystem.SALESFORCE
                    ? state.salesforceItem() != null
                    : state.netsuiteItem() != null;
        }

        LocalDateTime sourceModified = modified(state, route.sourceSystem());
        LocalDateTime targetModified = modified(state, route.targetSystem());

        return sourceModified != null && targetModified != null && targetModified.isAfter(sourceModified);
    }

    private LocalDateTime modified(ItemState state, SyncSystem system) {
        return switch(system) {
            case NETSUITE -> state.netsuiteItem() == null
                    ? null
                    : state.netsuiteItem().lastModifiedDate();
            case SALESFORCE -> state.salesforceItem() == null
                    ? null
                    : state.salesforceItem().lastModifiedDate();
            case SYSTEM -> null;
        };
     }
}
