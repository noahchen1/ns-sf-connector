package com.hamiltonjewelers.ns_sf_connector.service.sync.discovery.item;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.item.invLocation.NsItemClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.item.SfItemClient;
import com.hamiltonjewelers.ns_sf_connector.dto.ItemChange;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ItemChangeScanner {
    private final NsAuthClient nsAuthClient;
    private final SfAuthClient sfAuthClient;
    private final NsItemClient nsItemClient;
    private final SfItemClient sfItemClient;

    public ItemChangeScanner(
            NsAuthClient nsAuthClient,
            SfAuthClient sfAuthClient,
            NsItemClient nsItemClient,
            SfItemClient sfItemClient
    ) {
        this.nsAuthClient = nsAuthClient;
        this.sfAuthClient = sfAuthClient;
        this.nsItemClient = nsItemClient;
        this.sfItemClient = sfItemClient;
    }

    public List<ItemChange> scan(LocalDateTime since) {
        String netsuiteToken = nsAuthClient.fetchAccessToken();
        String salesforceToken = sfAuthClient.fetchAccessToken();

        Map<Integer, NsItemDto.ItemRecord> changedNetsuite = byNetsuiteId(
                nsItemClient.getItems(netsuiteToken, since),
                NsItemDto.ItemRecord::internalId
        );

        Map<Integer, SfItemDto.ItemRecord> linkedSalesforce = byNetsuiteId(
                sfItemClient.getItemsByNetsuiteIds(salesforceToken, changedNetsuite.keySet()),
                SfItemDto.ItemRecord::netsuiteId
        );

        Map<Integer, ItemChange> changes = new LinkedHashMap<>();

        changedNetsuite.forEach((netsuiteId, item) -> changes.put(
                netsuiteId,
                new ItemChange(
                        netsuiteId,
                        linkedSalesforce.containsKey(netsuiteId)
                                ? linkedSalesforce.get(netsuiteId).id()
                                : null,
                        item,
                        linkedSalesforce.get(netsuiteId)
                )
        ));

        List<SfItemDto.ItemRecord> changedSalesforceRows = sfItemClient.getItems(salesforceToken, since);
        Map<Integer, SfItemDto.ItemRecord> changedSalesforce = byNetsuiteId(
                changedSalesforceRows,
                SfItemDto.ItemRecord::netsuiteId
        );

        Map<Integer, NsItemDto.ItemRecord> linkedNetsuite = byNetsuiteId(
                nsItemClient.getItemsByInternalIds(netsuiteToken, changedSalesforce.keySet()),
                NsItemDto.ItemRecord::internalId
        );

        changedSalesforce.forEach((netsuiteId, item) -> changes.put(
                netsuiteId,
                new ItemChange(
                        netsuiteId,
                        item.id(),
                        linkedNetsuite.getOrDefault(
                                netsuiteId,
                                changes.containsKey(netsuiteId) ? changes.get(netsuiteId).netsuiteItem() : null
                        ),
                        item
                )
        ));

        List<ItemChange> discovered = new ArrayList<>(changes.values());
        changedSalesforceRows.stream()
                .filter(item -> item.netsuiteId() == null)
                .map(item -> new ItemChange(
                        null,
                        item.id(),
                        null,
                        item
                )).forEach(discovered::add);
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
