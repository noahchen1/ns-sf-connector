package com.hamiltonjewelers.ns_sf_connector.service.netsuite.authentication;

import com.hamiltonjewelers.ns_sf_connector.config.NsConfig;
import com.hamiltonjewelers.ns_sf_connector.service.jwt.JwtService;
import com.hamiltonjewelers.ns_sf_connector.service.jwt.NsKeyLoader;
import com.hamiltonjewelers.ns_sf_connector.utils.JoinUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;

@Service
public class NetsuiteAuthClient {
    private final NsConfig config;
    private final JwtService jwtService;

    @Autowired
    public NetsuiteAuthClient(NsConfig config, JwtService jwtService) {
        this.config = config;
        this.jwtService = jwtService;
    }

    public String fetchAccessToken() throws Exception {
        ECPrivateKey privateKey = NsKeyLoader.loadEcPrivateKey("ns.pem");

        String jwt = jwtService.createJwt(
                config.getClientId(),
                config.getCertId(),
                config.getTokenUrl(),
                JoinUtils.joinCommaStrings(config.getScope()),
                privateKey);

        String body = "grant_type=" + URLEncoder.encode("client_credentials", StandardCharsets.UTF_8)
                + "&client_assertion_type="
                + URLEncoder.encode("urn:ietf:params:oauth:client-assertion-type:jwt-bearer", StandardCharsets.UTF_8)
                + "&client_assertion=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getTokenUrl()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch access token: " + response.body());
        }

        return response.body();
    }
}
