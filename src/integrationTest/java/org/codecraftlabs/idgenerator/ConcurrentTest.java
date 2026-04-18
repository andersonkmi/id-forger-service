package org.codecraftlabs.idgenerator;

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
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Tag("integration")
public class ConcurrentTest {
    private static final String ENDPOINT_URL = "http://localhost:27110/idgenerator/v1/ids/default";
    private static final int TOTAL_CALLS = 100000;
    private static final int THREAD_POOL_SIZE = 100;

    @Test
    public void testConcurrentApiCalls() throws InterruptedException, ExecutionException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CompletionService<ApiCallResult> completionService =
                new ExecutorCompletionService<>(executor);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Submit all tasks
        for (int i = 1; i <= TOTAL_CALLS; i++) {
            final int callId = i;
            completionService.submit(() -> makeApiCall(client, callId));
        }

        // Collect results
        for (int i = 0; i < TOTAL_CALLS; i++) {
            Future<ApiCallResult> future = completionService.take();
            ApiCallResult result = future.get();

            if (result.isSuccess()) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
                System.out.println("Call " + result.getCallId() + " failed: " +
                        result.getErrorMessage());
            }

            responseTimes.add(result.getResponseTime());

            if ((i + 1) % 100 == 0) {
                System.out.println("Completed " + (i + 1) + " calls");
            }
        }

        executor.shutdown();
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        // Calculate statistics
        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        long maxResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        System.out.println("\n=== Results ===");
        System.out.println("Total calls: " + TOTAL_CALLS);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Total duration: " + totalDuration + "ms");
        System.out.println("Average response time: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("Max response time: " + maxResponseTime + "ms");
        System.out.println("Requests per second: " +
                String.format("%.2f", (TOTAL_CALLS * 1000.0) / totalDuration));
    }

    @Nonnull
    private ApiCallResult makeApiCall(HttpClient client, int callId) {
        long startTime = System.currentTimeMillis();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Load-Test-" + callId)
                    .GET() // Change to POST() with body if needed
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            long responseTime = System.currentTimeMillis() - startTime;

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new ApiCallResult(callId, true, responseTime, null, response.statusCode());
            } else {
                return new ApiCallResult(callId, false, responseTime,
                        "HTTP " + response.statusCode(), response.statusCode());
            }

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return new ApiCallResult(callId, false, responseTime, e.getMessage(), -1);
        }
    }
}
