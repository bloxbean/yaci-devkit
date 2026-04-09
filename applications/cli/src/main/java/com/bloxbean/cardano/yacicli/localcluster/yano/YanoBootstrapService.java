package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@Slf4j
public class YanoBootstrapService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean waitForReady(int httpPort, Consumer<String> writer) {
        String healthUrl = "http://localhost:" + httpPort + "/q/health/ready";
        int maxAttempts = 30;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(healthUrl))
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    writer.accept(success("Yano is ready"));
                    return true;
                }
            } catch (Exception e) {
                // Not ready yet
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
            writer.accept("Waiting for Yano HTTP API to be ready ...");
        }
        writer.accept(error("Yano HTTP API did not become ready within timeout"));
        return false;
    }

    public boolean shiftEpochs(int httpPort, int epochs, Consumer<String> writer) {
        String url = "http://localhost:" + httpPort + "/api/v1/devnet/epochs/shift";
        try {
            String body = objectMapper.writeValueAsString(Map.of("epochs", epochs));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                writer.accept(success("Shifted genesis back by %d epochs", epochs));
                log.debug("Shift response: {}", response.body());
                return true;
            } else {
                writer.accept(error("Failed to shift epochs: HTTP %d - %s", response.statusCode(), response.body()));
                return false;
            }
        } catch (Exception e) {
            writer.accept(error("Error shifting epochs: " + e.getMessage()));
            return false;
        }
    }

    public boolean catchUpToWallClock(int httpPort, Consumer<String> writer) {
        String url = "http://localhost:" + httpPort + "/api/v1/devnet/epochs/catch-up";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                writer.accept(success("Yano caught up to wall-clock time"));
                log.debug("Catch-up response: {}", response.body());
                return true;
            } else {
                writer.accept(error("Failed to catch up: HTTP %d - %s", response.statusCode(), response.body()));
                return false;
            }
        } catch (Exception e) {
            writer.accept(error("Error catching up to wall-clock: " + e.getMessage()));
            return false;
        }
    }

    public boolean fundAddress(int httpPort, String address, BigDecimal ada, Consumer<String> writer) {
        String url = "http://localhost:" + httpPort + "/api/v1/devnet/fund";
        try {
            String body = objectMapper.writeValueAsString(Map.of("address", address, "ada", ada));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.debug("Funded {} with {} ADA", address, ada);
                return true;
            } else {
                writer.accept(error("Failed to fund address: HTTP %d - %s", response.statusCode(), response.body()));
                return false;
            }
        } catch (Exception e) {
            writer.accept(error("Error funding address: " + e.getMessage()));
            return false;
        }
    }

    public String submitTx(int httpPort, String cborHex, Consumer<String> writer) {
        String url = "http://localhost:" + httpPort + "/api/v1/tx/submit";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(cborHex))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 202) {
                String txHash = response.body().replace("\"", "");
                log.debug("TX submitted: {}", txHash);
                return txHash;
            } else {
                writer.accept(error("TX submission failed: HTTP %d - %s", response.statusCode(), response.body()));
                return null;
            }
        } catch (Exception e) {
            writer.accept(error("Error submitting TX: " + e.getMessage()));
            return null;
        }
    }

    public JsonNode getEpochNonce(int httpPort) {
        String url = "http://localhost:" + httpPort + "/api/v1/node/epoch-nonce";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readTree(response.body());
            }
        } catch (Exception e) {
            log.debug("Error getting epoch nonce: {}", e.getMessage());
        }
        return null;
    }

    public boolean rollback(int httpPort, long blocks, Consumer<String> writer) {
        String url = "http://localhost:" + httpPort + "/api/v1/devnet/rollback";
        try {
            String body = objectMapper.writeValueAsString(Map.of("count", blocks));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                writer.accept(success("Rollback of %d blocks completed successfully", blocks));
                log.debug("Rollback response: {}", response.body());
                return true;
            } else {
                writer.accept(error("Failed to rollback: HTTP %d - %s", response.statusCode(), response.body()));
                return false;
            }
        } catch (Exception e) {
            writer.accept(error("Error during rollback: " + e.getMessage()));
            return false;
        }
    }

    public JsonNode getChainTip(int httpPort) {
        String url = "http://localhost:" + httpPort + "/api/v1/blocks/latest";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readTree(response.body());
            }
        } catch (Exception e) {
            log.debug("Error getting chain tip: {}", e.getMessage());
        }
        return null;
    }
}
