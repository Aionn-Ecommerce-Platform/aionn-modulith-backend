package com.aionn.identity.adapter.rest.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidJsonValidator implements ConstraintValidator<ValidJson, String> {

    // Single shared parser instance — ObjectMapper is thread-safe once
    // configured, and validation runs frequently in the request path.
    private static final ObjectMapper PARSER = new ObjectMapper();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null / blank is treated as "unset" and passes; pair with @NotBlank
        // when presence is required.
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            PARSER.readTree(value);
            return true;
        } catch (JsonProcessingException ex) {
            return false;
        }
    }
}
