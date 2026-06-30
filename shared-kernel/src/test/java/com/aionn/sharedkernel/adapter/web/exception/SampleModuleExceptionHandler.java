package com.aionn.sharedkernel.adapter.web.exception;

import org.springframework.http.HttpStatus;

public class SampleModuleExceptionHandler extends AbstractModuleExceptionHandler {

    public SampleModuleExceptionHandler() {
        registerErrors(HttpStatus.BAD_REQUEST, "SAMPLE_001");
        registerErrors(HttpStatus.NOT_FOUND, "SAMPLE_404");
    }

    public HttpStatus statusFor(String errorCode) {
        return resolveStatus(errorCode);
    }
}
