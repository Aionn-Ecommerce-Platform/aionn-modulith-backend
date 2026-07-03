package com.aionn.identity.application.port.out.registration;

public interface CaptchaTokenValidatorPort {
    boolean isValid(String captchaToken);
}
