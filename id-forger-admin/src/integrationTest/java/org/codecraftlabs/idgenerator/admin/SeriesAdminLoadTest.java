package org.codecraftlabs.idgenerator.admin;

import org.assertj.core.api.Assertions;
import org.codecraftlabs.idgenerator.common.loadtest.ApiCallResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Nonnull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Load test for the series management endpoints, run against an already running
 * ID Forger Admin service ({@code ./gradlew :id-forger-admin:loadTest}).
 *
 * <p>Only the read-only details endpoint is exercised, so the test never changes series
 * data. The admin endpoints are protected by a bulkhead that fast-fails excess calls with
 * {@code 503} and a {@code Retry-After} header. Under concurrency, every call must either
 * succeed or be rejected that way; anything else (timeouts, 5xx errors, missing headers)
 * fails the test.</p>
 */
@Tag("load")
public class SeriesAdminLoadTest {
    private static final String ENDPOINT_URL =
            System.getProperty("loadTest.baseUrl", "http://localhost:27111") + "/idgenerator/v1/series/default/details";
    private static final int TOTAL_CALLS = Integer.getInteger("loadTest.totalCalls", 2000);
    private static final int THREAD_POOL_SIZE = 50;

    @Test
    public void testConcurrentAdminCalls() throws InterruptedException, ExecutionException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CompletionService<ApiCallResult> completionService = new ExecutorCompletionService<>(executor);

        Map<Integer, Integer> statusCounts = new TreeMap<>();
        List<String> unexpected = new ArrayList<>();
        List<Long> responseTimes = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= TOTAL_CALLS; i++) {
            final int callId = i;
            completionService.submit(() -> makeApiCall(client, callId));
        }

        for (int i = 0; i < TOTAL_CALLS; i++) {
            Future<ApiCallResult> future = completionService.take();
            ApiCallResult result = future.get();

            statusCounts.merge(result.statusCode(), 1, Integer::sum);
            responseTimes.add(result.responseTime());
            if (result.errorMessage() != null) {
                unexpected.add("Call " + result.callId() + ": " + result.errorMessage());
            }
        }

        executor.shutdown();
        long totalDuration = System.currentTimeMillis() - startTime;

        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("\n=== Results ===");
        System.out.println("Total calls: " + TOTAL_CALLS);
        System.out.println("Status codes: " + statusCounts);
        System.out.println("Succeeded (200): " + statusCounts.getOrDefault(200, 0));
        System.out.println("Rejected by bulkhead (503): " + statusCounts.getOrDefault(503, 0));
        System.out.println("Unexpected: " + unexpected.size());
        System.out.println("Total duration: " + totalDuration + "ms");
        System.out.println("Average response time: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("Max response time: " + maxResponseTime + "ms");
        System.out.println("Requests per second: " +
                String.format("%.2f", (TOTAL_CALLS * 1000.0) / totalDuration));
        unexpected.stream().limit(20).forEach(System.out::println);

        Assertions.assertThat(unexpected).as("calls that were neither served nor rejected by the bulkhead").isEmpty();
        Assertions.assertThat(statusCounts.getOrDefault(200, 0)).as("successful calls").isPositive();
    }

    @Nonnull
    private ApiCallResult makeApiCall(HttpClient client, int callId) {
        long startTime = System.currentTimeMillis();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Load-Test-" + callId)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;
            int status = response.statusCode();

            if (status == 200) {
                return new ApiCallResult(callId, true, responseTime, null, status, response.body());
            }
            boolean bulkheadRejection = status == 503 && response.headers().firstValue("Retry-After").isPresent();
            String error = bulkheadRejection ? null : "HTTP " + status + ": " + response.body();
            return new ApiCallResult(callId, false, responseTime, error, status, response.body());
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return new ApiCallResult(callId, false, responseTime, e.getMessage(), -1, null);
        }
    }
}
