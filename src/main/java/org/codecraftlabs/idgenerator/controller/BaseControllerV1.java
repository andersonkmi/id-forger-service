package org.codecraftlabs.idgenerator.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Base REST controller for version 1 of the ID generator API.
 * Provides centralised validation-error handling shared by all subcontrollers.
 */
@RestController
@RequestMapping("/idgenerator/v1")
public class BaseControllerV1 {
    /**
     * Handles bean-validation failures and returns a map of field names to
     * their corresponding error messages with HTTP 400.
     *
     * @param ex the validation exception thrown by Spring MVC
     * @return map of {@code fieldName -> errorMessage} pairs
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}