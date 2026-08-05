package com.hamiltonjewelers.ns_sf_connector.service.sync.discovery.customer;

import com.hamiltonjewelers.ns_sf_connector.dto.CustomerChange;
import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CustomerChangeScanner {
    private final NsAuthClient nsAuthClient;
    private final SfAuthClient sfAuthClient;
    private final NsCustomerClient nsCustomerClient;
    private final SfAccountClient sfAccountClient;

    public CustomerChangeScanner(
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

    public List<CustomerChange> scan(LocalDateTime since) {
        String netsuiteToken = nsAuthClient.fetchAccessToken();
        String salesforceToken = sfAuthClient.fetchAccessToken();

        Map<Integer, CustomerDto> changedNetsuite = byNetsuiteId(
                nsCustomerClient.getCustomers(netsuiteToken, since),
                CustomerDto::internalId
        );
        Map<Integer, AccountDto.AccountRecord> linkedSalesforce = byNetsuiteId(
                sfAccountClient.getAccountsByNetsuiteIds(salesforceToken, changedNetsuite.keySet()),
                AccountDto.AccountRecord::netsuiteId
        );

        Map<Integer, CustomerChange> changes = new LinkedHashMap<>();
        changedNetsuite.forEach((netsuiteId, customer) -> changes.put(
                netsuiteId,
                new CustomerChange(
                        netsuiteId,
                        linkedSalesforce.containsKey(netsuiteId)
                                ? linkedSalesforce.get(netsuiteId).id()
                                : null,
                        customer,
                        linkedSalesforce.get(netsuiteId)
                )
        ));

        List<AccountDto.AccountRecord> changedSalesforceRows =
                sfAccountClient.getAccounts(salesforceToken, since);
        Map<Integer, AccountDto.AccountRecord> changedSalesforce = byNetsuiteId(
                changedSalesforceRows,
                AccountDto.AccountRecord::netsuiteId
        );
        Map<Integer, CustomerDto> linkedNetsuite = byNetsuiteId(
                nsCustomerClient.getCustomersByInternalIds(netsuiteToken, changedSalesforce.keySet()),
                CustomerDto::internalId
        );

        changedSalesforce.forEach((netsuiteId, account) -> changes.put(
                netsuiteId,
                new CustomerChange(
                        netsuiteId,
                        account.id(),
                        linkedNetsuite.getOrDefault(
                                netsuiteId,
                                changes.containsKey(netsuiteId) ? changes.get(netsuiteId).netsuiteCustomer() : null
                        ),
                        account
                )
        ));

        List<CustomerChange> discovered = new ArrayList<>(changes.values());
        changedSalesforceRows.stream()
                .filter(account -> account.netsuiteId() == null)
                .map(account -> new CustomerChange(null, account.id(), null, account))
                .forEach(discovered::add);
        return List.copyOf(discovered);
    }

    private <T> Map<Integer, T> byNetsuiteId(List<T> records, Function<T, Integer> idExtractor) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        return records.stream()
                .filter(record -> idExtractor.apply(record) != null)
                .collect(Collectors.toMap(idExtractor, Function.identity(), (first, ignored) -> first));
    }
}
