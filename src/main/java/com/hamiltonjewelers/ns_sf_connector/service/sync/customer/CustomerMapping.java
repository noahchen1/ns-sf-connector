package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

import com.hamiltonjewelers.ns_sf_connector.config.NsConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class CustomerMapping {
    private final NsConfig nsConfig;

    public CustomerMapping(NsConfig nsConfig) {
        this.nsConfig = nsConfig;
    }

    public Map<String, Object> salesforceCreateFields(CustomerDto customer) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("Name", customerName(customer));
        fields.put("First_Name__c", customer.firstname());
        fields.put("Last_Name__c", customer.lastname());
        fields.put("Netsuite_Id__c", String.valueOf(customer.internalId()));
        fields.put("Account_Email__c", customer.email());
        return fields;
    }

    public Map<String, Object> netsuiteCreateFields(AccountDto.AccountRecord account) {
        String salesforceId = requireText(account.id(), "Salesforce Account ID");
        String subsidiaryId = requireText(
                nsConfig.getDefaultCustomerSubsidiaryId(),
                "NetSuite default customer subsidiary ID"
        );
        String firstName = trimToNull(account.firstName());
        String lastName = trimToNull(account.lastName());
        String accountName = trimToNull(account.name());

        Map<String, Object> fields = new HashMap<>();
        fields.put("externalId", salesforceId);
        fields.put("custentity_sfid", salesforceId);
        fields.put("subsidiary", Map.of("id", subsidiaryId));

        if (firstName != null || lastName != null) {
            fields.put("isPerson", true);
            if (firstName != null) {
                fields.put("firstName", firstName);
            }
            fields.put("lastName", lastName != null
                    ? lastName
                    : requireText(accountName, "Salesforce Account name"));
        } else {
            fields.put("isPerson", false);
            fields.put("companyName", requireText(accountName, "Salesforce Account name"));
        }

        String email = trimToNull(account.email());
        if (email != null) {
            fields.put("email", email);
        }
        return fields;
    }

    public String netsuiteValue(String key, CustomerDto customer) {
        return switch (key) {
            case "name" -> customerName(customer);
            case "firstName" -> customer.firstname();
            case "lastName" -> customer.lastname();
            case "email" -> customer.email();
            default -> throw new IllegalArgumentException("Unsupported NetSuite customer field key: " + key);
        };
    }

    public String salesforceValue(String key, AccountDto.AccountRecord account) {
        return switch (key) {
            case "name" -> account.name();
            case "firstName" -> account.firstName();
            case "lastName" -> account.lastName();
            case "email" -> account.email();
            default -> throw new IllegalArgumentException("Unsupported Salesforce account field key: " + key);
        };
    }

    private String customerName(CustomerDto customer) {
        String first = Objects.toString(customer.firstname(), "").trim();
        String last = Objects.toString(customer.lastname(), "").trim();
        String name = (first + " " + last).trim();
        return name.isBlank() ? customer.custId() : name;
    }

    private String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalStateException(fieldName + " must be configured");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
