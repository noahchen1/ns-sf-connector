package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

import com.hamiltonjewelers.ns_sf_connector.config.ConflictResolutionConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class CustomerPatchPlanner {
    private final ConflictResolutionConfig conflictResolutionConfig;
    private final CustomerMapping mapping;

    public CustomerPatchPlanner(ConflictResolutionConfig conflictResolutionConfig, CustomerMapping mapping) {
        this.conflictResolutionConfig = conflictResolutionConfig;
        this.mapping = mapping;
    }

    public CustomerPatches plan(CustomerDto netsuite, AccountDto.AccountRecord salesforce, boolean netsuiteWins) {
        Map<String, Object> salesforcePatch = new HashMap<>();
        Map<String, Object> netsuitePatch = new HashMap<>();

        for (ConflictResolutionConfig.CustomerField field : conflictResolutionConfig.getCustomerFields()) {
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
        return new CustomerPatches(salesforcePatch, netsuitePatch);
    }
}
