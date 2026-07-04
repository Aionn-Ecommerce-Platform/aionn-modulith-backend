package com.aionn.identity.adapter.rest.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidJsonValidator.class)
public @interface ValidJson {

    String message() default "Value must be well-formed JSON";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
