package org.codecraftlabs.idgenerator;

public class ApiCallResult {
    private final int callId;
    private final boolean success;
    private final long responseTime;
    private final String errorMessage;
    private final int statusCode;

    public ApiCallResult(int callId, boolean success, long responseTime,
                         String errorMessage, int statusCode) {
        this.callId = callId;
        this.success = success;
        this.responseTime = responseTime;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
    }

    // Getters
    public int getCallId() { return callId; }
    public boolean isSuccess() { return success; }
    public long getResponseTime() { return responseTime; }
    public String getErrorMessage() { return errorMessage; }
    public int getStatusCode() { return statusCode; }
}
