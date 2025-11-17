package com.hamiltonjewelers.ns_sf_connector.client.ns.auth;

import com.hamiltonjewelers.ns_sf_connector.config.NsConfig;
import com.hamiltonjewelers.ns_sf_connector.utils.CheckRequire;
import com.hamiltonjewelers.ns_sf_connector.utils.JoinUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.interfaces.ECPrivateKey;

@Service
public class NsAuthClient {
    private final NsConfig config;
    private final JwtService jwtService;
    private final WebClient webClient;

    @Autowired
    public NsAuthClient(NsConfig config, JwtService jwtService, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.jwtService = jwtService;
        this.webClient = webClientBuilder.build();
    }

    public String fetchAccessToken() {
        return webClient
                .post()
                .uri(config.getAuthUrl())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData(buildNsAuthForm()))
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

    public MultiValueMap<String, String> buildNsAuthForm() {
        ECPrivateKey privateKey;
        String jwt;

        CheckRequire.requireNonBlank(config.getClientId(), "netsuite.clientId");
        CheckRequire.requireNonBlank(config.getCertId(), "netsuite.certId");
        CheckRequire.requireNonBlank(config.getAuthUrl(), "netsuite.tokenUrl");
        CheckRequire.requireNonBlank(config.getGrantType(), "netsuite.grantType");
        CheckRequire.requireNonBlank(config.getClientAssertionType(), "netsuite.clientAssertionType");

        try {
            privateKey = NsKeyLoader.loadEcPrivateKey("ns.pem");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key", e);
        }

        try {
            jwt = jwtService.createJwt(
                    config.getClientId(),
                    config.getCertId(),
                    config.getAuthUrl(),
                    JoinUtils.joinCommaStrings(config.getScope()),
                    privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JWT", e);
        }

        if (jwt == null || jwt.isEmpty()) throw new IllegalStateException("JWT cannot be empty");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", config.getGrantType());
        form.add("client_assertion_type", config.getClientAssertionType());
        form.add("client_assertion", jwt);

        return form;
    }
}
