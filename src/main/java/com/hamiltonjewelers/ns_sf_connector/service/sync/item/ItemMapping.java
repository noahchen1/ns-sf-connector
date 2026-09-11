package com.hamiltonjewelers.ns_sf_connector.service.sync.item;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ItemMapping {
    public Map<String, Object> salesforceCreateFields(NsItemDto.ItemRecord item) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("Name", item.name());
        fields.put("Netsuite_Id__c", item.internalId());
        fields.put("Display_Name__c", item.displayName());
        fields.put("Vendor_Item_Number__c", item.vendorNum());
        return fields;
    }

    public Map<String, Object> netsuiteCreateFields(SfItemDto.ItemRecord item) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("externalId", item.id());
        fields.put("custitem_sfid", item.id());
        fields.put("itemId", item.name());
        fields.put("displayName", item.displayName());
        fields.put("vendorName", item.vendorNum());
        return fields;
    }

    public String netsuiteValue(String key, NsItemDto.ItemRecord item) {
        return switch (key) {
            case "name" -> item.name();
            case "displayName" -> item.displayName();
            case "vendorNum" -> item.vendorNum();
            default -> throw new IllegalArgumentException("Unsupported NetSuite item field key: " + key);
        };
    }

    public String salesforceValue(String key, SfItemDto.ItemRecord item) {
        return switch (key) {
            case "name" -> item.name();
            case "displayName" -> item.displayName();
            case "vendorNum" -> item.vendorNum();
            default -> throw new IllegalArgumentException("Unsupported Salesforce item field key: " + key);
        };
    }
}
