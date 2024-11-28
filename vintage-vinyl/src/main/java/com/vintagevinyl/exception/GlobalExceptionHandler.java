package com.vintagevinyl.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.ui.Model;

import java.nio.file.AccessDeniedException;

/**
 * Global exception handler for the application.
 * Centralizes exception handling logic to improve code maintainability and provide consistent error handling.
 */
@ControllerAdvice // Allows this class to handle exceptions across the entire application.
public class GlobalExceptionHandler {

    /**
     * Handle generic exceptions.
     * This method catches all unhandled exceptions and displays a generic error message.
     *
     * @param e     The exception that was thrown.
     * @param model The model object to pass data to the error view.
     * @return The name of the error view.
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        // Add the exception message to the model to display it in the error view.
        model.addAttribute("error", e.getMessage());
        return "error"; // Return a generic error page.
    }

    /**
     * Handle access denied exceptions specifically.
     * Provides a user-friendly message when the user attempts to access a restricted resource.
     *
     * @param e     The AccessDeniedException that was thrown.
     * @param model The model object to pass data to the error view.
     * @return The name of the error view.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException e, Model model) {
        // Add a custom access-denied message to the model for better user experience.
        model.addAttribute("error", "Access denied: You do not have permission to access this resource.");
        return "error"; // Return the same error page, but with a specific message for access denial.
    }
}
