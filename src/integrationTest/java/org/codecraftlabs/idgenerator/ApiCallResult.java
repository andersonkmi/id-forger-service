package org.codecraftlabs.idgenerator;

public record ApiCallResult(int callId, boolean success, long responseTime,
                            String errorMessage, int statusCode) {
}
