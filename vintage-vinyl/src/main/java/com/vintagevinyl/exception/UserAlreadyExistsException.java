package com.vintagevinyl.exception;

// Custom exception for handling cases when a user already exists
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
