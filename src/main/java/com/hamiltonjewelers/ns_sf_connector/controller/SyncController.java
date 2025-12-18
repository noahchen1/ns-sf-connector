package com.hamiltonjewelers.ns_sf_connector.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.SyncJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sync")
public class SyncController {
    @Autowired
    private NsCustomerClient nsCustomerClient;
    @Autowired
    private SfAccountClient sfAccountClient;
    @Autowired
    private NsAuthClient nsAuthClient;
    @Autowired
    private SfAuthClient sfAuthClient;

    @Autowired
    private SyncJobService syncJobService;


    @PostMapping
    public void syncClients() throws JsonProcessingException {
//        String nsToken = nsAuthClient.fetchAccessToken();
//        String sfToken = sfAuthClient.fetchAccessToken();
//
//        List<CustomerItemDto> customers = nsCustomerClient.getCustomers(nsToken);
//        List<AccountDto.AccountRecord> accounts = sfAccountClient.getAccounts(sfToken);
//
//        Map<Integer, CustomerItemDto> nsMap = customers.stream()
//                .collect(Collectors.toMap(CustomerDto::getInternalId, c -> c));
//
//        Map<Integer, AccountDto.AccountRecord> sfMap = accounts.stream()
//                .collect(Collectors.toMap(AccountDto.AccountRecord::getNetsuiteId, c -> c));
//
//        List<CustomerItemDto> toInsertIntoSf = nsMap.keySet().stream()
//                .filter(netsuiteId -> !sfMap.containsKey(netsuiteId))
//                .map(nsMap::get)
//                .toList();
//
//        Map<String, Object> accountFields = Map.of(
//                "Name", "Test Account",
//                "First_Name__c", "First",
//                "Last_Name__c", "Last",
//                "Netsuite_Id__c", "123456",
//                "Account_Email__c", "testemail@gmail.com"
//        );
//
//        sfAccountClient.createAccount(sfToken, accountFields);


        Map<String, Object> payload = new HashMap<>();
        payload.put("customerName", "John Doe");
        payload.put("email", "john.doe@example.com");
        ObjectMapper objectMapper = new ObjectMapper();


        SyncJob syncJob = new SyncJob();
        syncJob.setSourceSystem("ERP");
        syncJob.setTargetSystem("CRM");
        syncJob.setRecordType("Customer");
        syncJob.setSourceRecordId("SRC12345");
        syncJob.setTargetRecordId("TGT67890");
        syncJob.setSyncType("FULL");
        syncJob.setOperation("INSERT");
        syncJob.setGroupingKey("batch-20240610");
        syncJob.setPriority(5);
        syncJob.setStatus("PENDING");
        syncJob.setAttemptCount(0);
        syncJob.setMaxAttempts(5);
        syncJob.setAvailableAt(LocalDateTime.parse("2024-06-10T12:00:00"));
        syncJob.setClaimedAt(null);
        syncJob.setClaimedBy(null);
        syncJob.setPayload(objectMapper.writeValueAsString(payload));
        syncJob.setErrorMessage(null);

        syncJobService.createSyncJob(syncJob);

    }

//    private boolean recordsAreEqual(CustomerItemDto customers, AccountDto.AccountRecord accounts) {
//
//    }
}


