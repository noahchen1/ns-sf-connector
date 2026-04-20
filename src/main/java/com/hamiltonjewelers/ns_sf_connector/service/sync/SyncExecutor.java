package com.hamiltonjewelers.ns_sf_connector.service.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class SyncExecutor {
    private final NsAuthClient nsAuthClient;
    private final SfAuthClient sfAuthClient;
    private final NsCustomerClient nsCustomerClient;
    private final SfAccountClient sfAccountClient;

    public SyncExecutor(
            NsAuthClient nsAuthClient,
            SfAuthClient sfAuthClient,
            NsCustomerClient nsCustomerClient,
            SfAccountClient sfAccountClient
    ) {
        this.nsAuthClient = nsAuthClient;
        this.sfAuthClient = sfAuthClient;
        this.nsCustomerClient = nsCustomerClient;
        this.sfAccountClient = sfAccountClient;
    }

    public void execute(SyncJob job) {
        if (job == null) {
            throw new IllegalArgumentException("job must not be null");
        }

        PreflightOutcome outcome = preflight(job);

        if (outcome == PreflightOutcome.STALE_TARGET_CHANGED) {
            // mark current job stale/superseded in service layer
            // then create reconcile job
            syncJobService.markSuperseded(job.getId(), "Target changed since enqueue");
            syncJobService.enqueueReconcileJob(
                    job.getRecordType(),
                    job.getSourceRecordId(),
                    job.getTargetRecordId(),
                    job.getId()
            );
            return;
        }

        String route = getSyncRoute(job);
        switch (route) {
            case "NETSUITE->SALESFORCE:CUSTOMER:INSERT" -> syncCustomerInsert(job);
            case "NETSUITE->SALESFORCE:CUSTOMER:UPDATE" -> syncCustomerUpdate(job);
            case "SALESFORCE->NETSUITE:CUSTOMER:UPDATE" -> syncCustomerUpdateFromSalesforce(job);
            case "SYSTEM->SYSTEM:CUSTOMER:RECONCILE" -> reconcileCustomer(job);
            default -> throw new UnsupportedOperationException("Unsupported sync route: " + route);
        }
    }

    private PreflightOutcome preflight(SyncJob job) {
        String route = getSyncRoute(job);

        Integer nsId = resolveNetsuiteId(job);
        if (nsId == null) return PreflightOutcome.OK;

        LatestState latest = fetchLatestState(nsId);
        LocalDateTime sourceNow = getSourceLastModified(job, latest);
        LocalDateTime targetNow = getTargetLastModified(job, latest);

        if (targetNow != null && targetNow.isAfter(sourceNow)) {
            return PreflightOutcome.STALE_TARGET_CHANGED;
        }

        return PreflightOutcome.OK;
    }

    private LatestState fetchLatestState(int nsId) {
        String nsToken = nsAuthClient.fetchAccessToken();
        String sfToken = sfAuthClient.fetchAccessToken();

        CustomerItemDto ns = null;
        List<CustomerItemDto> nsRows = nsCustomerClient.getCustomer(nsToken, String.valueOf(nsId));
        if (nsRows != null && !nsRows.isEmpty()) ns = nsRows.getFirst();

        AccountDto.AccountRecord sf = null;
        List<AccountDto.AccountRecord> sfRows = sfAccountClient.getAccountsByNetsuiteIds(sfToken, Set.of(nsId));
        if (sfRows != null && !sfRows.isEmpty()) sf = sfRows.getFirst();

        return new LatestState(ns, sf);
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

    private void reconcileCustomer(SyncJob job) {
        Integer nsId = resolveNetsuiteId(job);
        if (nsId == null) throw new IllegalStateException("Cannot reconcile without NS id");

        LatestState latest = fetchLatestState(nsId);
        CustomerDto ns = latest.ns;
        AccountDto.AccountRecord sf = latest.sf;

        if (ns == null || sf == null) {
            // fallback: create missing side via existing flows
            return;
        }

        // Merge rule:
        // - Different field changes -> merge
        // - Same field changed on both -> NetSuite wins
        Map<String, Object> sfPatch = new HashMap<>();
        Map<String, Object> nsPatch = new HashMap<>();

        // You can replace baseline with persisted prior sync snapshot if available.
        // For now, treat NS as authoritative when conflict/ambiguity.
        resolveField("First_Name__c", ns.getFirstname(), sf.getFirstName(), sfPatch, nsPatch);
        resolveField("Last_Name__c", ns.getLastname(), sf.getLastName(), sfPatch, nsPatch);
        resolveField("Account_Email__c", ns.getEmail(), sf.getEmail(), sfPatch, nsPatch);

        String sfToken = sfAuthClient.fetchAccessToken();
        if (!sfPatch.isEmpty()) {
            sfAccountClient.updateAccount(sfToken, sf.getId(), sfPatch);
        }

        String nsToken = nsAuthClient.fetchAccessToken();
        if (!nsPatch.isEmpty()) {
            nsCustomerClient.updateCustomer(nsToken, String.valueOf(nsId), nsPatch);
        }
    }

    private void resolveField(
            String sfField,
            String nsValue,
            String sfValue,
            Map<String, Object> sfPatch,
            Map<String, Object> nsPatch
    ) {
        // Without baseline, treat disagreement as conflict -> NS wins.
        if (!Objects.equals(nsValue, sfValue)) {
            sfPatch.put(sfField, nsValue);
        }
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
        String sfToken = sfAuthClient.fetchAccessToken();

        CustomerItemDto customer = nsCustomerClient.getCustomer(nsToken,
                job.getSourceRecordId()).getFirst();

        System.out.printf("Found customer with id: %s%n",
                job.getSourceRecordId());

        System.out.printf("Customer read to be processed: %s%n", customer);

        Map<String, Object> accountFields = new HashMap<>();
        accountFields.put("Name",
                customer.getFirstname() + " " + customer.getLastname());
        accountFields.put("First_Name__c", customer.getFirstname());
        accountFields.put("Last_Name__c", customer.getLastname());
        accountFields.put("Netsuite_Id__c", String.valueOf(customer.getInternalId()));
        accountFields.put("Account_Email__c", customer.getEmail());

        String response = sfAccountClient.createAccount(sfToken, accountFields);
    }



    private LocalDateTime getSourceLastModified(SyncJob job, LatestState latest) {
        String source = normalize(job.getSourceSystem());
        if ("NETSUITE".equals(source)) return latest.ns != null ? latest.ns.getLastModifiedDate() : null;
        if ("SALESFORCE".equals(source)) return latest.sf != null ? latest.sf.getLastModifiedDate() : null;
        return null;
    }

    private LocalDateTime getTargetLastModified(SyncJob job, LatestState latest) {
        String target = normalize(job.getTargetSystem());
        if ("NETSUITE".equals(target)) return latest.ns != null ? latest.ns.getLastModifiedDate() : null;
        if ("SALESFORCE".equals(target)) return latest.sf != null ? latest.sf.getLastModifiedDate() : null;
        return null;
    }

    private Integer resolveNetsuiteId(SyncJob job) {
        try {
            if ("NETSUITE".equals(normalize(job.getSourceSystem()))) {
                return Integer.parseInt(job.getSourceRecordId());
            }
            if (job.getTargetRecordId() != null) {
                return Integer.parseInt(job.getTargetRecordId());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private enum PreflightOutcome {
        OK,
        STALE_TARGET_CHANGED
    }

    private static class LatestState {
        private final CustomerDto ns;
        private final AccountDto.AccountRecord sf;

        private LatestState(CustomerDto ns, AccountDto.AccountRecord sf) {
            this.ns = ns;
            this.sf = sf;
        }
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

