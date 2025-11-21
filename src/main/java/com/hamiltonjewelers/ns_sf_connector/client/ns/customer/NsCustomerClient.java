package com.hamiltonjewelers.ns_sf_connector.client.ns.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.config.NsConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerResDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class NsCustomerClient {
    final private NsConfig config;
    final private WebClient webClient;

    public NsCustomerClient(NsConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
    }

    public List<CustomerItemDto> getCustomers(String accessToken) {
        final String queryStr = """
                    SELECT
                    customer.id AS internalId,
                    customer.entityId AS custId,
                    customer.lastName AS lastname,
                    customer.firstName AS firstname,
                    customer.email AS email,
                    CustomerSubsidiaryRelationship.subsidiary AS subsidiary,
                    entityAddress.addrText AS address
                    FROM
                    customer
                    LEFT JOIN CustomerSubsidiaryRelationship ON Customer.ID = CustomerSubsidiaryRelationship.entity
                    AND CustomerSubsidiaryRelationship.isprimarysub = 'T'
                    LEFT JOIN entityAddressbook ON entityAddressbook.entity = customer.id
                    AND entityAddressbook.defaultbilling = 'T'
                    LEFT JOIN entityAddress ON entityAddress.nkey = entityAddressbook.AddressBookAddress
                    LEFT JOIN employee ON employee.id = customer.salesrep
                    WHERE
                    TO_DATE (customer.datecreated, 'MM-DD-YYYY') BETWEEN '11-01-2025' AND TO_DATE  (SYSDATE, 'MM-DD-YYYY')
                    ORDER BY
                    customer.datecreated DESC
                """;
        final String formmatedQuery = String.format("{\"q\": \"%s\"}", queryStr.replaceAll("\\s+", " ").trim());

        CustomerResDto res = webClient
                .post()
                .uri("/query/v1/suiteql")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "transient")
                .bodyValue(formmatedQuery)
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
                ).bodyToMono(CustomerResDto.class)
                .block();

        if (res == null) {
            throw new RuntimeException("Failed to fetch ns customers: empty response");
        }

        return res.getItems();
    }
}
