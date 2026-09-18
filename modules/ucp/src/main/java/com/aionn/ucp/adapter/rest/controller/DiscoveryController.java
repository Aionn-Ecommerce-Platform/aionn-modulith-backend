package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.dto.BusinessProfileResponse;
import com.aionn.ucp.application.port.out.BusinessProfileValidationPort;
import com.aionn.ucp.infrastructure.config.UcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiscoveryController {

    private final BusinessProfileResponse profile;

    @Autowired
    public DiscoveryController(BusinessProfileValidationPort validator,
            @Autowired(required = false) UcpProperties properties) {
        String version = properties != null ? properties.version() : BusinessProfileResponse.PROTOCOL_VERSION;
        // In Phase 1, only advertise capabilities that are implemented and verified.
        profile = BusinessProfileResponse.of(version, Map.of(), Map.of(), Map.of());
        validator.validate(new ObjectMapper().valueToTree(profile));
    }

    public DiscoveryController(BusinessProfileValidationPort validator) {
        this(validator, null);
    }

    @GetMapping(value = "/.well-known/ucp", produces = MediaType.APPLICATION_JSON_VALUE)
    public BusinessProfileResponse getProfile() {
        // Advertise no commerce services until their complete protocol flows are
        // available.
        return profile;
    }
}
