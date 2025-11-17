package com.hamiltonjewelers.ns_sf_connector.client.sf.auth;

import com.hamiltonjewelers.ns_sf_connector.config.SfConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
public class SfAuthClient {
    private final SfConfig config;
    private final WebClient webClient;

    @Autowired
    public SfAuthClient(SfConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.webClient = webClientBuilder.build();
    }

    public String fetchAccessToken() {
        return webClient
                .post()
                .uri(config.getAuthUrl())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData(buildSfAuthForm()))
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

    public MultiValueMap<String, String> buildSfAuthForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", config.getGrantType());
        form.add("client_id", config.getClientId());
        form.add("client_secret", config.getClientSecret());

        return form;
    }
}
