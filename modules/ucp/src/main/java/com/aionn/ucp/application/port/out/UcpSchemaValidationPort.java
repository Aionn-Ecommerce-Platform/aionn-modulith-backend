package com.aionn.ucp.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;

public interface UcpSchemaValidationPort {

    void validate(String schemaUri, JsonNode payload);

    void validateErrorResponse(JsonNode errorResponse);
}
