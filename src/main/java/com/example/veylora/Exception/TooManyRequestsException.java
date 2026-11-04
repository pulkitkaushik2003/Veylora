package com.example.veylora.Exception;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException() {
        super("Too many attempts. Try again in a minute.");
    }
}