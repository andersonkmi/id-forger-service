package org.codecraftlabs.idgenerator.common.loadtest;

/**
 * Outcome of a single HTTP call made by a load test.
 *
 * @param callId       sequential identifier of the call
 * @param success      {@code true} if the call returned a 2xx status
 * @param responseTime elapsed time in milliseconds
 * @param errorMessage failure description, or {@code null} on success
 * @param statusCode   HTTP status code, or {@code -1} if no response was received
 * @param body         response body, or {@code null} if no response was received
 */
public record ApiCallResult(int callId, boolean success, long responseTime,
                            String errorMessage, int statusCode, String body) {
}
