package com.hamiltonjewelers.ns_sf_connector.service.sync.item;

import com.hamiltonjewelers.ns_sf_connector.dto.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import org.springframework.stereotype.Component;

@Component
public class ItemLinkResolver {
    public int resolveNetsuiteItemId(SyncJob job, SyncRoute route) {
        String candidate;

        if (route.sourceSystem() == SyncSystem.NETSUITE || route.sourceSystem() == SyncSystem.SYSTEM) {
            candidate = job.getSourceRecordId();
        } else if (route.targetSystem() == SyncSystem.NETSUITE) {
            candidate = job.getTargetRecordId();
        } else {
            throw new IllegalArgumentException("Route does not identify a NetSuite item: " + route);
        }

        if (candidate == null || candidate.isBlank()) {
            throw new IllegalArgumentException("Cannot resolve NetSuite item ID for sync job " + job.getId());
        }

        try {
            return Integer.parseInt(candidate);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid NetSuite item ID: " + candidate, e);
        }
    }
}
