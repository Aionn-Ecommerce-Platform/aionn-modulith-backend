package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.dto.BusinessProfileResponse;
import com.aionn.ucp.application.port.out.BusinessProfileValidationPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiscoveryController {

    private final BusinessProfileResponse profile;

    public DiscoveryController(BusinessProfileValidationPort validator) {
        profile = BusinessProfileResponse.discoveryOnly();
        validator.validate(new ObjectMapper().valueToTree(profile));
    }

    @GetMapping(value = "/.well-known/ucp", produces = MediaType.APPLICATION_JSON_VALUE)
    public BusinessProfileResponse getProfile() {
        // Advertise no commerce services until their complete protocol flows are available.
        return profile;
    }
}
