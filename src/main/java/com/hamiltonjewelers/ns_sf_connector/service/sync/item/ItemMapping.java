package com.hamiltonjewelers.ns_sf_connector.service.sync.item;

import com.hamiltonjewelers.ns_sf_connector.config.NsConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ItemMapping {
    private final NsConfig nsConfig;

    public ItemMapping(NsConfig nsConfig){ this.nsConfig = nsConfig; }

    public Map<String, Object> salesforceCreateFields(SfItemDto.ItemRecord item) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("")
    }
}
