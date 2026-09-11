package com.hamiltonjewelers.ns_sf_connector.service.sync.item.conflict;

import com.hamiltonjewelers.ns_sf_connector.dto.ItemPatches;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ItemConflictResolver {
    private final ItemPatchPlanner patchPlanner;

    public ItemConflictResolver(ItemPatchPlanner patchPlanner) {
        this.patchPlanner = patchPlanner;
    }

    public ItemPatches resolve(NsItemDto.ItemRecord netsuite, SfItemDto.ItemRecord salesforce) {
        return patchPlanner.plan(
                netsuite,
                salesforce,
                netsuiteWins(netsuite.lastModifiedDate(), salesforce.lastModifiedDate())
        );
    }

    private boolean netsuiteWins(LocalDateTime netsuiteModified, LocalDateTime salesforceModified) {
        return salesforceModified == null
                || (netsuiteModified != null && !salesforceModified.isAfter(netsuiteModified));
    }
}
