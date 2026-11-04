package com.example.veylora.Exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() {
        super("Not authenticated");
    }
}