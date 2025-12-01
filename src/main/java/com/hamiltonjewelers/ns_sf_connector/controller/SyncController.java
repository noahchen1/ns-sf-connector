package com.hamiltonjewelers.ns_sf_connector.controller;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


    @PostMapping
    public void syncClients() {
        String nsToken = nsAuthClient.fetchAccessToken();
        String sfToken = sfAuthClient.fetchAccessToken();

        List<CustomerItemDto> customers = nsCustomerClient.getCustomers(nsToken);
        List<AccountDto.AccountRecord> accounts = sfAccountClient.getAccounts(sfToken);

        Map<Integer, CustomerItemDto> nsMap = customers.stream()
                .collect(Collectors.toMap(CustomerDto::getInternalId, c -> c));

        Map<Integer, AccountDto.AccountRecord> sfMap = accounts.stream()
                .collect(Collectors.toMap(AccountDto.AccountRecord::getNetsuiteId, c -> c));

        List<CustomerItemDto> toInsertIntoSf = nsMap.keySet().stream()
                .filter(netsuiteId -> !sfMap.containsKey(netsuiteId))
                .map(nsMap::get)
                .toList();

        System.out.println(toInsertIntoSf);

//        [CustomerItem{internalId='382547', cust_id='CUST169754', email='nstestcust1201@gmail.com', firstname='Nstest', lastname='Customer1201', subsidiary='30', address='Nstest Customer1201
//            92 NASSAU ST
//            PRINCETON NJ 08542-4519
//            United States'}, CustomerItem{internalId='382447', cust_id='CUST169753', email='null', firstname='Test', lastname='User Acc 2', subsidiary='30', address='Test User Acc 2
//            United States'}, CustomerItem{internalId='382345', cust_id='CUST169750', email='null', firstname='Test', lastname='Customer11262', subsidiary='30', address='Test Customer11262
//            90 NASSAU ST
//            PRINCETON NJ 08542-4520
//            United States'}, CustomerItem{internalId='382347', cust_id='CUST169752', email='asdf@gmail.com', firstname='test7', lastname='test9', subsidiary='1', address='test7 test9
//            123 fake st
//            United States'}, CustomerItem{internalId='382346', cust_id='CUST169751', email='null', firstname='Test', lastname='Customer11263', subsidiary='30', address='Test Customer11263
//            90 NASSAU ST
//            PRINCETON NJ 08542-4520
//            United States'}]
    }

//    private boolean recordsAreEqual(CustomerItemDto customers, AccountDto.AccountRecord accounts) {
//
//    }
}
