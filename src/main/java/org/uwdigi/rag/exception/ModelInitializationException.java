package org.uwdigi.rag.exception;

public class ModelInitializationException extends RuntimeException {
    public ModelInitializationException(String message) {
        super(message);
    }

    public ModelInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}