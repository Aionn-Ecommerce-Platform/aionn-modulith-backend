package com.aionn.identity.adapter.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.DateTimeException;
import java.time.ZoneId;

public class ValidTimezoneValidator implements ConstraintValidator<ValidTimezone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null / blank is treated as "unset" and passes — pair with @NotBlank
        // when presence is required. ZoneId.of() also accepts fixed offsets
        // ("+07:00") and short ids ("UTC"/"Z") in addition to IANA region
        // names — that's intentional here; the constraint rejects strings
        // that ZoneId can't resolve at all (e.g. "Foo/Bar").
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            ZoneId.of(value);
            return true;
        } catch (DateTimeException ex) {
            return false;
        }
    }
}
