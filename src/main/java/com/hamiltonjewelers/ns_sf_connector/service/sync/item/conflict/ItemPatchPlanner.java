package com.hamiltonjewelers.ns_sf_connector.service.sync.item.conflict;

import com.hamiltonjewelers.ns_sf_connector.config.ConflictResolutionConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemPatches;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;
import com.hamiltonjewelers.ns_sf_connector.service.sync.item.ItemMapping;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class ItemPatchPlanner {
    private final ConflictResolutionConfig conflictResolutionConfig;
    private final ItemMapping mapping;

    public ItemPatchPlanner(ConflictResolutionConfig conflictResolutionConfig, ItemMapping mapping) {
        this.conflictResolutionConfig = conflictResolutionConfig;
        this.mapping = mapping;
    }

    public ItemPatches plan(NsItemDto.ItemRecord netsuite, SfItemDto.ItemRecord salesforce, boolean netsuiteWins) {
        Map<String, Object> salesforcePatch = new HashMap<>();
        Map<String, Object> netsuitePatch = new HashMap<>();
        for (ConflictResolutionConfig.ItemField field : conflictResolutionConfig.getItemFields()) {
            String netsuiteValue = mapping.netsuiteValue(field.getKey(), netsuite);
            String salesforceValue = mapping.salesforceValue(field.getKey(), salesforce);
            if (!Objects.equals(netsuiteValue, salesforceValue)) {
                if (netsuiteWins) {
                    salesforcePatch.put(field.getSalesforceField(), netsuiteValue);
                } else {
                    netsuitePatch.put(field.getNetsuiteField(), salesforceValue);
                }
            }
        }
        return new ItemPatches(
                Collections.unmodifiableMap(salesforcePatch),
                Collections.unmodifiableMap(netsuitePatch)
        );
    }
}
