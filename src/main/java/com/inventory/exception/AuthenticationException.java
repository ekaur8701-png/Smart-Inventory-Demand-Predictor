package com.inventory.exception;

public class AuthenticationException extends RuntimeException {
    private final int status;

    public AuthenticationException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
