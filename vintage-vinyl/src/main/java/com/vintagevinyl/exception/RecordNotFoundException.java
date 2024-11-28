package com.vintagevinyl.exception;

/**
 * Custom exception to indicate that a specific record was not found in the system.
 * This exception extends RuntimeException, making it an unchecked exception.
 * It provides a clear mechanism to signal record-related errors during runtime.
 */
public class RecordNotFoundException extends RuntimeException {

    /**
     * Constructs a new RecordNotFoundException with the specified detail message.
     *
     * @param message The detail message explaining why the exception was thrown.
     */
    public RecordNotFoundException(String message) {
        super(message); // Pass the message to the superclass constructor for error reporting.
    }
}
