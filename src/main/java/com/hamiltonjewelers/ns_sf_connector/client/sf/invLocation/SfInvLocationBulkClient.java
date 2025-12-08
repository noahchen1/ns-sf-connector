package com.hamiltonjewelers.ns_sf_connector.client.sf.invLocation;

import com.hamiltonjewelers.ns_sf_connector.config.SfConfig;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class SfInvLocationBulkClient {

    private final WebClient webClient;

    public SfInvLocationBulkClient(SfConfig config, WebClient.Builder builder) {
        this.webClient = builder.baseUrl(config.getBaseUrl()).build();
    }

    public String createJob(String accessToken) {
        return webClient
                .post()
                .uri("/data/v64.0/jobs/ingest")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(Map.of(
                        "object", "Inventory_Location__c",
                        "operation", "insert",
                        "contentType", "CSV",
                        "lineEnding", "LF"
                ))
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
                .bodyToMono(Map.class)
                .map(res -> res.get("id").toString())
                .block();
    }

    public void uploadCsv(String accessToken, String jobId, Path csvPath) {
        try {
            webClient
                    .put()
                    .uri("/data/v64.0/jobs/ingest/{jobId}/batches", jobId)
                    .header("Content-Type", "text/csv")
                    .header("Authorization", "Bearer " + accessToken)
                    .bodyValue(Files.readAllBytes(csvPath))
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void closeJob(String accessToken, String jobId) {
        webClient.patch()
                .uri("/data/v64.0/jobs/ingest/{jobId}", jobId)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(Map.of("state", "UploadComplete"))
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
                .toBodilessEntity()
                .block();
    }


    public void waitForCompletion(String accessToken, String jobId) {
        while (true) {
            Map<?, ?> res = webClient.get()
                    .uri("/data/v64.0/jobs/ingest/{jobId}", jobId)
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
                    .bodyToMono(Map.class)
                    .block();

            String state = res.get("state").toString();
            System.out.println("Job State: " + state);

            if (state.equals("JobComplete") || state.equals("Failed") || state.equals("Aborted")) {
                break;
            }

            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
        }
    }

    public String getFailedResults(String accessToken, String jobId) {
        return webClient.get()
                .uri("/data/v64.0/jobs/ingest/{jobId}/failedResults", jobId)
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
