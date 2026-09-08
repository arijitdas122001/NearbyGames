package com.gameconnect.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain-level business rule violation. Map to a specific HTTP status/code.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
