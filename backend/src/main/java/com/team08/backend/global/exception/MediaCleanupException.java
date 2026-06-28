package com.team08.backend.global.exception;

public class MediaCleanupException extends RuntimeException {
    public MediaCleanupException(String message) {
        super(message);
    }

    public MediaCleanupException(String message, Throwable cause) {
        super(message, cause);
    }
}
