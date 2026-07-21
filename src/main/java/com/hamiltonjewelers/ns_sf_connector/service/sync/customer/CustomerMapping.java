package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class CustomerMapping {
    public Map<String, Object> salesforceCreateFields(CustomerDto customer) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("Name", customerName(customer));
        fields.put("First_Name__c", customer.getFirstname());
        fields.put("Last_Name__c", customer.getLastname());
        fields.put("Netsuite_Id__c", String.valueOf(customer.getInternalId()));
        fields.put("Account_Email__c", customer.getEmail());
        return fields;
    }

    public String netsuiteValue(String key, CustomerDto customer) {
        return switch (key) {
            case "name" -> customerName(customer);
            case "firstName" -> customer.getFirstname();
            case "lastName" -> customer.getLastname();
            case "email" -> customer.getEmail();
            default -> throw new IllegalArgumentException("Unsupported NetSuite customer field key: " + key);
        };
    }

    public String salesforceValue(String key, AccountDto.AccountRecord account) {
        return switch (key) {
            case "name" -> account.getName();
            case "firstName" -> account.getFirstName();
            case "lastName" -> account.getLastName();
            case "email" -> account.getEmail();
            default -> throw new IllegalArgumentException("Unsupported Salesforce account field key: " + key);
        };
    }

    private String customerName(CustomerDto customer) {
        String first = Objects.toString(customer.getFirstname(), "").trim();
        String last = Objects.toString(customer.getLastname(), "").trim();
        String name = (first + " " + last).trim();
        return name.isBlank() ? customer.getCustId() : name;
    }
}
