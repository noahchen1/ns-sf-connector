package com.hamiltonjewelers.ns_sf_connector.client.sf.item;

import com.hamiltonjewelers.ns_sf_connector.config.SfConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.item.SfItemDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SfItemClient {
    final private WebClient webClient;

    public SfItemClient(SfConfig config, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
    }

    public List<SfItemDto.ItemRecord> getItems(String accessToken, LocalDateTime since) {
        final String formattedDate = since.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

        final String queryStr = """
                    SELECT
                    Id,
                    Name,
                    Netsuite_Id__c,
                    Display_Name__c,
                    Vendor_Item_Number__c,
                    LastModifiedDate
                    FROM Netsuite_Item__c
                    WHERE LastModifiedDate >= %s
                    LIMIT 5
                """.formatted(formattedDate);

        return executeQuery(queryStr, accessToken);
    }

    public List<SfItemDto.ItemRecord> getItemsByNetsuiteIds(String accessToken, Set<Integer> netsuiteIds) {
        if (netsuiteIds == null || netsuiteIds.isEmpty()) {
            return List.of();
        }

        String idList = netsuiteIds.stream()
                .map(netsuiteId -> "'" + String.valueOf(netsuiteId) + "'")
                .collect(Collectors.joining(", "));

        final String queryStr = """
                    SELECT
                    Id,
                    Name,
                    Netsuite_Id__c,
                    Display_Name__c,
                    Vendor_Item_Number__c,
                    LastModifiedDate
                    FROM Netsuite_Item__c
                    WHERE Netsuite_Id__c IN (%s)
                """.formatted(idList);

        return executeQuery(queryStr, accessToken);
    }

    public SfItemDto.ItemRecord getItemById(String accessToken, String itemId) {
        if (itemId == null || !itemId.matches("[a-zA-Z0-9]{15,18}")) {
            throw new IllegalArgumentException("Invalid Salesforce Item ID: " + itemId);
        }

        final String queryStr = """
                    SELECT
                    Id,
                    Name,
                    Netsuite_Id__c,
                    Display_Name__c,
                    Vendor_Item_Number__c,
                    LastModifiedDate
                    FROM Netsuite_Item__c
                    WHERE Id = '%s'
                    LIMIT 1
                """.formatted(itemId);

        List<SfItemDto.ItemRecord> items = executeQuery(queryStr, accessToken);

        if (items.isEmpty()) {
            throw new IllegalStateException("Salesforce Item not found: " + itemId);
        }

        return items.getFirst();
    }

    public String createItem(String accessToken, Map<String, Object> itemFields) {
        SfItemDto.CreateResult result = webClient.post()
                .uri("/data/v64.0/sobjects/Netsuite_Item__c")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(itemFields)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException(
                                "Salesforce Item creation failed: " + response.statusCode() + " - " + body))))
                .bodyToMono(SfItemDto.CreateResult.class)
                .block();

        if (result == null || !result.success() || result.id() == null || result.id().isBlank()) {
            throw new IllegalStateException("Salesforce Item creation response did not include a successful record ID");
        }
        return result.id();
    }

    public void updateItem(String accessToken, String itemId, Map<String, Object> itemFields) {
        webClient.patch()
                .uri("/data/v64.0/sobjects/Netsuite_Item__c/{itemId}", itemId)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(itemFields)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException(
                                "Salesforce Item update failed: " + response.statusCode() + " - " + body))))
                .toBodilessEntity()
                .block();
    }

    private List<SfItemDto.ItemRecord> executeQuery(String queryStr, String accessToken) {
        SfItemDto res = webClient
                .get()
                .uri("/data/v64.0/query", uriBuilder -> uriBuilder
                        .queryParam("q", queryStr.replaceAll("\\s+", " ").trim())
                        .build())
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body ->
                                        Mono.error(new RuntimeException("Client Error: " + response.statusCode() + " - " + body))
                                )
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body ->
                                        Mono.error(new RuntimeException("Server error: " + response.statusCode() + " - " + body))
                                )
                )
                .bodyToMono(SfItemDto.class)
                .block();

        if (res == null) {
            throw new RuntimeException("Failed to fetch accounts: empty response");
        }

        return res.records() != null ? res.records() : Collections.emptyList();
    }
}
