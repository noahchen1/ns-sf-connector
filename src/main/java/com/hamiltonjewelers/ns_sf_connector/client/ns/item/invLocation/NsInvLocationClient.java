package com.hamiltonjewelers.ns_sf_connector.client.ns.item.invLocation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.config.NsConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.invLocation.NsInvLocationResponseDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
public class NsInvLocationClient {
    final private WebClient webClient;

    public NsInvLocationClient(NsConfig config, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
    }

    public List<NsInvLocationResponseDto.InvLocation> getInvLocation(String accessToken) {
        List<NsInvLocationResponseDto.InvLocation> allResults = new ArrayList<>();
        long lastSeenItem = 0;
        boolean hasMore;

        ObjectMapper mapper = new ObjectMapper();

        do {
            String queryStr = String.format("""
                    SELECT
                        aggregateItemLocation.item AS item,
                        aggregateItemLocation.location AS location,
                        NVL(aggregateItemLocation.quantityOnHand, 0) AS onhandqty
                    FROM
                        aggregateItemLocation
                    WHERE
                        aggregateItemLocation.item > %d
                    ORDER BY
                        aggregateItemLocation.item ASC
                    """, lastSeenItem);

            String formattedQuery = String.format("{\"q\": \"%s\"}", queryStr.replaceAll("\\s+", " ").trim());

            NsInvLocationResponseDto res = webClient
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query/v1/suiteql")
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "transient")
                    .bodyValue(formattedQuery)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException("Client Error: " + response.statusCode() + " - " + body)))
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException("Server error: " + response.statusCode() + " - " + body)))
                    )
                    .bodyToMono(NsInvLocationResponseDto.class)
                    .block();

            if (res == null) {
                throw new RuntimeException("Failed to fetch ns inventory location: empty response");
            }

            try {
                System.out.println("API Response: " + mapper.writeValueAsString(res));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            List<NsInvLocationResponseDto.InvLocation> results = res.getInvLocations();
            if (!results.isEmpty()) {
                allResults.addAll(results);
                lastSeenItem = results.getLast().getItem();

                System.out.println(lastSeenItem);
            }

            hasMore = !results.isEmpty();
        } while (hasMore);

        return allResults;
    }

}
