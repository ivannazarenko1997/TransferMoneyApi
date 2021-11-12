package com.db.awmd.challenge.exception;

public class OverDraftException extends RuntimeException {
    public OverDraftException(String message) {
        super(message);
    }
}