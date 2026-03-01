package com.eyarko.ecom.infrastructure.web;

/**
 * Cached idempotency response entry keyed by Idempotency-Key.
 */
public class IdempotencyEntry {
    private String requestHash;
    private int httpStatus;
    private String responseBody;

    public IdempotencyEntry() {
    }

    public IdempotencyEntry(String requestHash, int httpStatus, String responseBody) {
        this.requestHash = requestHash;
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}

