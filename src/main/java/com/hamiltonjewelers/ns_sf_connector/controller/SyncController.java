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
    }
}
