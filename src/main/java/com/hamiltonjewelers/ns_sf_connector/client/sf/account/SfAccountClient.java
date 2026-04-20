package com.hamiltonjewelers.ns_sf_connector.client.sf.account;

import com.hamiltonjewelers.ns_sf_connector.config.SfConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SfAccountClient {
    final private SfConfig config;
    final private WebClient webClient;

    public SfAccountClient(SfConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
    }

    public List<AccountDto.AccountRecord> getAccounts(String accessToken, LocalDateTime since) {
        final String formattedDate = since
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

        final String queryStr = """
                SELECT
                Id,
                Netsuite_Id__c,
                First_Name__c,
                Last_Name__c,
                Account_Email__c,
                LastModifiedDate
                FROM ACCOUNT
                WHERE LastModifiedDate >= %s
                LIMIT 5
            """.formatted(formattedDate);

            return executeQuery(queryStr, accessToken);
    }

    public List<AccountDto.AccountRecord> getAccountsByNetsuiteIds(String accessToken, Set<Integer> netsuiteIds) {
        if (netsuiteIds == null || netsuiteIds.isEmpty()) {
            return List.of();
        }

        String idList = netsuiteIds.stream()
                .map(netsuiteId -> "'" + String.valueOf(netsuiteId) + "'")
                .collect(Collectors.joining(", "));

        final String queryStr = """
            SELECT
            Id,
            Netsuite_Id__c,
            First_Name__c,
            Last_Name__c,
            Account_Email__c,
            LastModifiedDate
            FROM Account
            WHERE Netsuite_Id__c IN (%s)
        """.formatted(idList);

        return executeQuery(queryStr, accessToken);
    }

    public String createAccount(String accessToken, Map<String, Object> accountFields) {
        String res = webClient
                .post()
                .uri("/data/v64.0/sobjects/Account")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(accountFields)
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
                .bodyToMono(String.class)
                .block();

        System.out.printf("SF Account Creation Response: %s%n", res);

//        {"id":"001bm00001fincnAAA","success":true,"errors":[]}
        return res;
    }

    private List<AccountDto.AccountRecord> executeQuery(String queryStr, String accessToken) {
        AccountDto res = webClient
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
                .bodyToMono(AccountDto.class)
                .block();

        System.out.println(res);

        if (res == null) {
            throw new RuntimeException("Failed to fetch accounts: empty response");
        }

        return res.getRecords() != null ? res.getRecords() : Collections.emptyList();
    }
}
