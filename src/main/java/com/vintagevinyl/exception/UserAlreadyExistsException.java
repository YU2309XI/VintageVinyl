package com.vintagevinyl.exception;

/**
 * Custom exception to indicate that a user already exists in the system.
 * This exception extends RuntimeException, making it an unchecked exception.
 * It is used to handle scenarios where duplicate user registration attempts occur.
 */
public class UserAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new UserAlreadyExistsException with the specified detail message.
     *
     * @param message The detail message explaining the cause of the exception.
     */
    public UserAlreadyExistsException(String message) {
        super(message); // Pass the message to the superclass constructor for detailed error reporting.
    }
}
