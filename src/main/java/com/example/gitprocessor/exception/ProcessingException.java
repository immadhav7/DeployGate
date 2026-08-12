package com.example.gitprocessor.exception;

/**
 * Thrown when Excel processing fails due to a bad file, unsupported format,
 * or an I/O error during output generation.
 */
public class ProcessingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProcessingException(String message) {
        super(message);
    }

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
