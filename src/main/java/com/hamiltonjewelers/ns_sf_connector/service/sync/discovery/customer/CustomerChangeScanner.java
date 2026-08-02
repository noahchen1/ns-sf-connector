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
                new CustomerChange(netsuiteId, customer, linkedSalesforce.get(netsuiteId))
        ));

        Map<Integer, AccountDto.AccountRecord> changedSalesforce = byNetsuiteId(
                sfAccountClient.getAccounts(salesforceToken, since),
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
                        linkedNetsuite.getOrDefault(
                                netsuiteId,
                                changes.containsKey(netsuiteId) ? changes.get(netsuiteId).netsuiteCustomer() : null
                        ),
                        account
                )
        ));
        return List.copyOf(changes.values());
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
