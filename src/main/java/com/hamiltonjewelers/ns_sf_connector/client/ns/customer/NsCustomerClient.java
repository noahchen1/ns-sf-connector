package com.hamiltonjewelers.ns_sf_connector.client.ns.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerResDto;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class NsCustomerClient {
    public List<CustomerItemDto> getCustomers(String accessToken) {
        final String url = "https://5405357-sb1.suitetalk.api.netsuite.com/services/rest/query/v1/suiteql";
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

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "transient")
                    .POST(HttpRequest.BodyPublishers.ofString(formmatedQuery))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            CustomerResDto parsedRes = mapper.readValue(response.body(), CustomerResDto.class);

            System.out.println(parsedRes);

            return parsedRes.getItems();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
