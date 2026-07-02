package com.aionn.identity.adapter.rest.dto.registration.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InitiateRegistrationRequest(
                @NotBlank(message = "Phone number is required") @Pattern(regexp = "^(\\d{10}|\\+[1-9]\\d{7,14})$", message = "Phone number must be 10 digits or valid E.164 format") String phoneNumber,

                @NotBlank(message = "Captcha token is required") String captchaToken) {
}


