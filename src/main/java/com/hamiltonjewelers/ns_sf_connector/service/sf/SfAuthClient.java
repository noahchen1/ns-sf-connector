package com.hamiltonjewelers.ns_sf_connector.service.sf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.config.SfConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.TokenResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpRequest;

@Service
public class SfAuthClient {
    private final SfConfig config;

    @Autowired
    public SfAuthClient(SfConfig config) {
        this.config = config;
    }

    public String fetchAccessToken() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String body = "grant_type="
                + URLEncoder.encode(config.getGrantType(), StandardCharsets.UTF_8)
                + "&client_id="
                + URLEncoder.encode(config.getClientId(), StandardCharsets.UTF_8)
                + "&client_secret="
                + URLEncoder.encode(config.getClientSecret(), StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getAuthUrl()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch access token: " + response.body());
        }

        return response.body();
    }
}
