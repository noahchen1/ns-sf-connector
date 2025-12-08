package com.hamiltonjewelers.ns_sf_connector.client.sf.invLocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.config.SfConfig;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class SfInvLocationClient {
    final private WebClient webClient;

    public SfInvLocationClient(SfConfig sfConfig, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(sfConfig.getBaseUrl()).build();
    }

    public String createInvLocation(String accessToken, Map<String, Object>invLocationFields) {
        System.out.println(accessToken);
        ObjectMapper mapper = new ObjectMapper();

        String res = webClient
                .post()
                .uri("/data/v64.0/sobjects/Inventory_Location__c")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(invLocationFields)
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

        return res;
    }
}
