package com.backend.backendapi.web;

import org.springframework.http.HttpStatus;

/**
 * Excepcion de negocio con el status HTTP que debe devolverse.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
