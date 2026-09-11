package com.hamiltonjewelers.ns_sf_connector.client.ns.item.invLocation;

import com.hamiltonjewelers.ns_sf_connector.config.NsConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.item.NsItemDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class NsItemClient {
    final private WebClient webClient;

    public NsItemClient(NsConfig config, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
    }

    public List<NsItemDto.ItemRecord> getItems(String accessToken, LocalDateTime since) {
        final String formattedDate = since.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
        final String queryStr = """
                SELECT TOP(5)
                    item.id AS internalid,
                    item.itemid AS itemid,
                    item.displayname AS displayname,
                    item.vendorname AS vendorname,
                    item.custitem_sfid AS sfid,
                    TO_CHAR(item.lastmodifieddate, 'YYYY-MM-DD HH24:MI:SS') AS lastmodifieddate
                FROM item
                WHERE item.lastmodifieddate >= TO_DATE('%s', 'MM/DD/YYYY HH24:MI:SS')
                ORDER BY
                item.lastmodifieddate DESC
                """.formatted(formattedDate);

        return executeQuery(queryStr, accessToken);
    }

    public List<NsItemDto.ItemRecord> getItem(String accessToken, String internalId) {
        final String queryStr = """
                SELECT
                    item.id AS internalid,
                    item.itemid AS itemid,
                    item.displayname AS displayname,
                    item.vendorname AS vendorname,
                    item.custitem_sfid AS sfid,
                    TO_CHAR(item.lastmodifieddate, 'YYYY-MM-DD HH24:MI:SS') AS lastmodifieddate
                FROM item
                WHERE item.id = %s
                """.formatted(internalId);

        return executeQuery(queryStr, accessToken);
    }

    public List<NsItemDto.ItemRecord> getItemsBySalesforceId(String accessToken, String salesforceId) {
        if (salesforceId == null || !salesforceId.matches("[a-zA-Z0-9]{15,18}")) {
            throw new IllegalArgumentException("Invalid Salesforce Item ID: " + salesforceId);
        }

        final String queryStr = """
                SELECT TOP(5)
                    item.id AS internalid,
                    item.itemid AS itemid,
                    item.displayname AS displayname,
                    item.vendorname AS vendorname,
                    item.custitem_sfid AS sfid,
                    TO_CHAR(item.lastmodifieddate, 'YYYY-MM-DD HH24:MI:SS') AS lastmodifieddate
                FROM item
                WHERE item.custitem_sfid = '%s'
        """.formatted(salesforceId);

        return executeQuery(queryStr, accessToken);
    }

    public List<NsItemDto.ItemRecord> getItemsByInternalIds(String accessToken, Set<Integer> internalIds) {
        if (internalIds == null || internalIds.isEmpty()) {
            return Collections.emptyList();
        }

        String idList = internalIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        final String queryStr = """
                SELECT TOP(5)
                    item.id AS internalid,
                    item.itemid AS itemid,
                    item.displayname AS displayname,
                    item.vendorname AS vendorname,
                    item.custitem_sfid AS sfid,
                    TO_CHAR(item.lastmodifieddate, 'YYYY-MM-DD HH24:MI:SS') AS lastmodifieddate
                FROM item
                WHERE item.id IN (%s)
        """.formatted(idList);

        return executeQuery(queryStr, accessToken);
    }

    public void updateItem(String accessToken, String internalId, Map<String, Object> itemFields) {
        webClient.patch()
                .uri("/record/v1/inventoryItem/{internalId}", internalId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(itemFields)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException(
                                "NetSuite Item update failed: " + response.statusCode() + " - " + body))))
                .toBodilessEntity()
                .block();
    }

    public int createItem(String accessToken, Map<String, Object> itemFields) {
        ResponseEntity<Void> response = webClient.post()
                .uri("/record/v1/inventoryItem")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(itemFields)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "NetSuite Item creation failed: "
                                                + clientResponse.statusCode() + " - " + body))))
                .toBodilessEntity()
                .block();

        if (response == null || response.getHeaders().getLocation() == null) {
            throw new IllegalStateException("NetSuite Item creation response did not include a Location header");
        }
        return internalIdFrom(response.getHeaders().getLocation());
    }

    private List<NsItemDto.ItemRecord> executeQuery(String queryStr, String accessToken) {
        Map<String, String> requestBody = Map.of(
                "q",
                queryStr.replaceAll("\\s+", " ").trim()
        );

        NsItemDto res = webClient
                .post()
                .uri("/query/v1/suiteql")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .header("Prefer", "transient")
                .bodyValue(requestBody)
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
                .bodyToMono(NsItemDto.class)
                .block();

        if (res == null) {
            throw new IllegalStateException("Failed to fetch NetSuite items: empty response");
        }

        return res.items() != null ? res.items() : Collections.emptyList();
    }

    private int internalIdFrom(URI location) {
        String path = location.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid NetSuite Item Location header: " + location, exception);
        }
    }
}
