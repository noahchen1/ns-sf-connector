package com.hamiltonjewelers.ns_sf_connector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Field mappings used when a Customer and Account need reconciliation.
 * The record-level LastModifiedDate decides which side wins because the
 * source APIs do not currently expose field-level modification timestamps.
 */
@Component
@ConfigurationProperties(prefix = "app.sync.conflict-resolution")
public class ConflictResolutionConfig {
    private List<CustomerField> customerFields = new ArrayList<>();

    public List<CustomerField> getCustomerFields() {
        return customerFields;
    }

    public void setCustomerFields(List<CustomerField> customerFields) {
        this.customerFields = customerFields == null ? new ArrayList<>() : customerFields;
    }

    public static class CustomerField {
        /** Canonical property understood by CustomerMapping: name, firstName, lastName, or email. */
        private String key;
        /** NetSuite Record API field ID to update when Salesforce wins. */
        private String netsuiteField;
        /** Salesforce Account field API name to update when NetSuite wins. */
        private String salesforceField;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getNetsuiteField() { return netsuiteField; }
        public void setNetsuiteField(String netsuiteField) { this.netsuiteField = netsuiteField; }
        public String getSalesforceField() { return salesforceField; }
        public void setSalesforceField(String salesforceField) { this.salesforceField = salesforceField; }
    }
}
