package com.hamiltonjewelers.ns_sf_connector.service.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SyncExecutor {
    private final NsAuthClient nsAuthClient;
    private final SfAuthClient sfAuthClient;
    private final NsCustomerClient nsCustomerClient;

    public SyncExecutor(
            NsAuthClient nsAuthClient,
            SfAuthClient sfAuthClient,
            NsCustomerClient nsCustomerClient
    ) {
        this.nsAuthClient = nsAuthClient;
        this.sfAuthClient = sfAuthClient;
        this.nsCustomerClient = nsCustomerClient;
    }

    public void execute(SyncJob job) {
        if (job == null) {
            throw new IllegalArgumentException("job must not be null");
        }
        String syncRoute = getSyncRoute(job);
        switch (syncRoute) {
            case "NETSUITE->SALESFORCE:CUSTOMER:INSERT" -> syncCustomerInsert(job);

            default -> throw new UnsupportedOperationException("Unsupported sync route: " + syncRoute);
        }
    }

    private String getSyncRoute(SyncJob job) {
        return normalize(job.getSourceSystem())
                + "->"
                + normalize(job.getTargetSystem())
                + ":"
                + normalize(job.getRecordType())
                + ":"
                + normalize(job.getOperation());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase().replace(' ',
                '_');
    }

    private void syncCustomerInsert(SyncJob job) {
        int nsInternalId;

        try {
            nsInternalId = Integer.parseInt(job.getSourceRecordId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid sourceRecordId for CUSTOMER INSERT: " + job.getSourceRecordId(), e
            );
        }

        String nsToken = nsAuthClient.fetchAccessToken();

        CustomerItemDto customer = nsCustomerClient.getCustomer(nsToken,
                job.getSourceRecordId()).getFirst();

        System.out.printf("Found customer with id: %s",
                job.getSourceRecordId());

        System.out.printf("Customer read to be processed: %s", customer);
    }
}






//package com.hamiltonjewelers.ns_sf_connector.service.sync;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
//import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
//import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
//import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
//import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
//import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//public class SyncExecutor {
//
//    private final NsAuthClient nsAuthClient;
//    private final SfAuthClient sfAuthClient;
//    private final NsCustomerClient nsCustomerClient;
//    private final SfAccountClient sfAccountClient;
//    private final ObjectMapper objectMapper;
//
//    public SyncExecutor(
//            NsAuthClient nsAuthClient,
//            SfAuthClient sfAuthClient,
//            NsCustomerClient nsCustomerClient,
//            SfAccountClient sfAccountClient,
//            ObjectMapper objectMapper
//    ) {
//        this.nsAuthClient = nsAuthClient;
//        this.sfAuthClient = sfAuthClient;
//        this.nsCustomerClient = nsCustomerClient;
//        this.sfAccountClient = sfAccountClient;
//        this.objectMapper = objectMapper;
//    }
//
//    public void execute(SyncJob job) {
//        if (job == null) {
//            throw new IllegalArgumentException("job must not be null");
//        }
//
//        String route = routeKey(job);
//        switch (route) {
//            case "NETSUITE->SALESFORCE:CUSTOMER:INSERT" -> syncCustomerInsert(job);
//            // Add more routes as you support them:
//            // case "NETSUITE->SALESFORCE:INV_LOCATION:INSERT" -> syncInvLocationInsert(job);
//            default -> throw new UnsupportedOperationException("Unsupported sync route: " + route);
//        }
//    }
//
//    private String routeKey(SyncJob job) {
//        return normalize(job.getSourceSystem())
//                + "->"
//                + normalize(job.getTargetSystem())
//                + ":"
//                + normalize(job.getRecordType())
//                + ":"
//                + normalize(job.getOperation());
//    }
//
//    private String normalize(String value) {
//        return value == null ? "" : value.trim().toUpperCase().replace(' ', '_');
//    }
//
//    private void syncCustomerInsert(SyncJob job) {
//        int nsInternalId;
//        try {
//            nsInternalId = Integer.parseInt(job.getSourceRecordId());
//        } catch (NumberFormatException e) {
//            throw new IllegalArgumentException(
//                    "Invalid sourceRecordId for CUSTOMER INSERT: " + job.getSourceRecordId(), e
//            );
//        }
//
//        String nsToken = nsAuthClient.fetchAccessToken();
//        String sfToken = sfAuthClient.fetchAccessToken();
//
//        CustomerItemDto customer = nsCustomerClient.getCustomers(nsToken).stream()
//                .filter(c -> c.getInternalId() != null && c.getInternalId() == nsInternalId)
//                .findFirst()
//                .orElseThrow(() -> new IllegalStateException(
//                        "NetSuite customer not found for internalId=" + nsInternalId
//                ));
//
//        Map<String, Object> accountFields = new HashMap<>();
//        accountFields.put("Name", buildName(customer));
//        accountFields.put("First_Name__c", customer.getFirstname());
//        accountFields.put("Last_Name__c", customer.getLastname());
//        accountFields.put("Netsuite_Id__c", String.valueOf(customer.getInternalId()));
//        accountFields.put("Account_Email__c", customer.getEmail());
//
//        // Remove null/blank values to avoid Salesforce validation issues.
//        accountFields.entrySet().removeIf(e ->
//                e.getValue() == null || (e.getValue() instanceof String s && s.isBlank())
//        );
//
//        String createResponse = sfAccountClient.createAccount(sfToken, accountFields);
//        String sfId = extractSalesforceId(createResponse);
//
//        if (sfId != null && !sfId.isBlank()) {
//            // Let worker/service persist this field when marking COMPLETED.
//            job.setTargetRecordId(sfId);
//        }
//    }
//
//    private String buildName(CustomerItemDto customer) {
//        String first = customer.getFirstname() == null ? "" : customer.getFirstname().trim();
//        String last = customer.getLastname() == null ? "" : customer.getLastname().trim();
//        String full = (first + " " + last).trim();
//
//        if (!full.isBlank()) return full;
//        if (customer.getCustId() != null && !customer.getCustId().isBlank()) return customer.getCustId();
//        return "NS-" + customer.getInternalId();
//    }
//
//    private String extractSalesforceId(String responseBody) {
//        try {
//            JsonNode root = objectMapper.readTree(responseBody);
//            JsonNode idNode = root.path("id");
//            if (!idNode.isMissingNode() && !idNode.isNull()) {
//                return idNode.asText();
//            }
//            return null;
//        } catch (Exception e) {
//            throw new IllegalStateException("Could not parse Salesforce create response: " + responseBody, e);
//        }
//    }
//}

