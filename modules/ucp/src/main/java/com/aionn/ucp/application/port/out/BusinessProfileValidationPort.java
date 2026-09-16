package com.aionn.ucp.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;

public interface BusinessProfileValidationPort {

    void validate(JsonNode profile);
}
