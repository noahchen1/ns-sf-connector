package com.hamiltonjewelers.ns_sf_connector.client.sf.account;

import com.hamiltonjewelers.ns_sf_connector.config.SfConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class SfAccountClient {
    final private SfConfig config;
    final private WebClient webClient;

    public SfAccountClient(SfConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
    }

    public List<AccountDto.AccountRecord> getAccounts(String accessToken) {
        final String queryStr = """
                SELECT Id, Netsuite_Id__c, First_Name__c, Last_Name__c, Account_Email__c FROM ACCOUNT WHERE Netsuite_Id__c != null LIMIT 5
            """;

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

        return res.getRecords();
    }

}
