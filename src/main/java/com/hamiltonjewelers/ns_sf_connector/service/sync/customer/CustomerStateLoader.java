package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class CustomerStateLoader {
    private final NsAuthClient nsAuthClient;
    private final SfAuthClient sfAuthClient;
    private final NsCustomerClient nsCustomerClient;
    private final SfAccountClient sfAccountClient;

    public CustomerStateLoader(NsAuthClient nsAuthClient, SfAuthClient sfAuthClient,
                               NsCustomerClient nsCustomerClient, SfAccountClient sfAccountClient) {
        this.nsAuthClient = nsAuthClient;
        this.sfAuthClient = sfAuthClient;
        this.nsCustomerClient = nsCustomerClient;
        this.sfAccountClient = sfAccountClient;
    }

    public CustomerState load(int netsuiteId) {
        List<CustomerItemDto> nsRows = nsCustomerClient.getCustomer(
                nsAuthClient.fetchAccessToken(), String.valueOf(netsuiteId));
        List<AccountDto.AccountRecord> sfRows = sfAccountClient.getAccountsByNetsuiteIds(
                sfAuthClient.fetchAccessToken(), Set.of(netsuiteId));

        return new CustomerState(
                nsRows == null || nsRows.isEmpty() ? null : nsRows.getFirst(),
                sfRows == null || sfRows.isEmpty() ? null : sfRows.getFirst()
        );
    }
}
