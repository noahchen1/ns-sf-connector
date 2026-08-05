package com.hamiltonjewelers.ns_sf_connector.client.ns.customer;

import tools.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.config.NsConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerResDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class NsCustomerClient {
    final private NsConfig config;
    final private WebClient webClient;

    public NsCustomerClient(NsConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
    }

    public List<CustomerDto> getCustomers(String accessToken, LocalDateTime since) {
        final String formattedDate = since.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
        final String queryStr = """
                    SELECT TOP(5)
                    customer.id AS internalId,
                    customer.entityId AS custId,
                    customer.lastName AS lastname,
                    customer.firstName AS firstname,
                    customer.custentity_sfid AS sfid,
                    customer.email AS email,
                    TO_CHAR(customer.lastmodifieddate, 'YYYY-MM-DD HH24:MI:SS') AS lastmodifieddate,
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
                    customer.lastmodifieddate >= TO_DATE('%s', 'MM/DD/YYYY HH24:MI:SS')
                    ORDER BY
                    customer.datecreated DESC
                """.formatted(formattedDate);

        return executeQuery(queryStr, accessToken);
    }

    public List<CustomerDto> getCustomer(String accessToken, String internalId) {
        final String queryStr = """
                    SELECT
                    customer.id AS internalId,
                    customer.entityId AS custId,
                    customer.lastName AS lastname,
                    customer.firstName AS firstname,
                    customer.email AS email,
                    TO_CHAR(customer.lastmodifieddate, 'YYYY-MM-DD HH24:MI:SS') AS lastmodifieddate
                    FROM
                    customer
                    WHERE customer.id = %s
                """.formatted(internalId);

        return executeQuery(queryStr, accessToken);
    }

    public void updateCustomer(String accessToken, String internalId, Map<String, Object> customerFields) {
        webClient.patch()
                .uri("/record/v1/customer/{internalId}", internalId)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(customerFields)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException(
                                "NetSuite Customer update failed: " + response.statusCode() + " - " + body))))
                .toBodilessEntity()
                .block();
    }

    public int createCustomer(String accessToken, Map<String, Object> customerFields) {
        ResponseEntity<Void> response = webClient.post()
                .uri("/record/v1/customer")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(customerFields)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "NetSuite Customer creation failed: "
                                                + clientResponse.statusCode() + " - " + body))))
                .toBodilessEntity()
                .block();

        if (response == null || response.getHeaders().getLocation() == null) {
            throw new IllegalStateException(
                    "NetSuite Customer creation response did not include a Location header"
            );
        }
        return internalIdFrom(response.getHeaders().getLocation());
    }

    public List<CustomerDto> getCustomersBySalesforceId(
            String accessToken,
            String salesforceId
    ) {
        requireSalesforceId(salesforceId);
        final String queryStr = """
                SELECT
                customer.id AS internalId,
                customer.entityId AS custId,
                customer.lastName AS lastname,
                customer.firstName AS firstname,
                customer.custentity_sfid AS sfid,
                customer.email AS email,
                TO_CHAR(customer.lastmodifieddate, 'YYYY-MM-DD HH24:MI:SS') AS lastmodifieddate
                FROM customer
                WHERE customer.custentity_sfid = '%s'
            """.formatted(salesforceId);

        return executeQuery(queryStr, accessToken);
    }

    public List<CustomerDto> getCustomersByInternalIds(String accessToken, Set<Integer> internalIds) {
        if (internalIds == null || internalIds.isEmpty()) {
            return Collections.emptyList();
        }

        String idList = internalIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        final String queryStr = """
                SELECT
                customer.id AS internalId,
                customer.entityId AS custId,
                customer.lastName AS lastname,
                customer.firstName AS firstname,
                customer.custentity_sfid AS sfid,
                customer.email AS email,
                TO_CHAR(customer.lastmodifieddate, 'YYYY-MM-DD HH24:MI:SS') AS lastmodifieddate,
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
                WHERE customer.id IN (%s)
            """.formatted(idList);

        return executeQuery(queryStr, accessToken);
    }

    private List<CustomerDto> executeQuery(String queryStr, String accessToken) {
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

        return res.items() != null ? res.items() : Collections.emptyList();
    }

    private int internalIdFrom(URI location) {
        String path = location.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Invalid NetSuite Customer Location header: " + location,
                    exception
            );
        }
    }

    private void requireSalesforceId(String salesforceId) {
        if (salesforceId == null || !salesforceId.matches("[a-zA-Z0-9]{15,18}")) {
            throw new IllegalArgumentException("Invalid Salesforce Account ID: " + salesforceId);
        }
    }
}
