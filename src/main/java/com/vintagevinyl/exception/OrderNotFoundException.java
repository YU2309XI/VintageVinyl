package com.vintagevinyl.exception;

/**
 * Custom exception to indicate that a specific order was not found.
 * This exception extends RuntimeException, making it an unchecked exception.
 * It is used to provide a clear and specific error message when an order cannot be located in the system.
 */
public class OrderNotFoundException extends RuntimeException {

    /**
     * Constructs a new OrderNotFoundException with the specified detail message.
     *
     * @param message The detail message that describes the error.
     */
    public OrderNotFoundException(String message) {
        super(message); // Passes the message to the superclass constructor for error reporting.
    }
}
