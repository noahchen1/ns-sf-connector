package com.hamiltonjewelers.ns_sf_connector.client.sf.account;

import com.hamiltonjewelers.ns_sf_connector.config.SfConfig;
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
        this.webClient = webClientBuilder.build();
    }

    public String getAccounts(String accessToken) {
        final String queryStr = """
                SELECT FIELDS(All) FROM ACCOUNT ORDER BY Name LIMIT 5
            """;
        final String formmatedQuery = String.format("{\"q\": \"%s\"}", queryStr.replaceAll("\\s+", " ").trim());

        return webClient
                .get()
                .uri(config.getAccountUrl(), uriBuilder -> uriBuilder
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
                .bodyToMono(String.class)
                .block();
    }
}
