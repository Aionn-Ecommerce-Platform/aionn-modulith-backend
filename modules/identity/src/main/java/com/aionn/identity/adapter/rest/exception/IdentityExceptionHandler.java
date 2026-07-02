package com.aionn.identity.adapter.rest.exception;

import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.sharedkernel.adapter.web.exception.AbstractModuleExceptionHandler;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "com.aionn.identity.adapter.rest.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityExceptionHandler extends AbstractModuleExceptionHandler {

        public IdentityExceptionHandler() {
                registerErrors(HttpStatus.NOT_FOUND,
                                "IDENTITY_003", "IDENTITY_109", "IDENTITY_201", "IDENTITY_205",
                                "IDENTITY_208", "IDENTITY_215", "IDENTITY_217", "IDENTITY_218",
                                "IDENTITY_219", "IDENTITY_302", "IDENTITY_401", "IDENTITY_501",
                                "IDENTITY_601");
                registerErrors(HttpStatus.CONFLICT,
                                "IDENTITY_001", "IDENTITY_002", "IDENTITY_005", "IDENTITY_108",
                                "IDENTITY_110", "IDENTITY_112", "IDENTITY_207", "IDENTITY_214",
                                "IDENTITY_216", "IDENTITY_221", "IDENTITY_303", "IDENTITY_402",
                                "IDENTITY_403");
                registerErrors(HttpStatus.UNAUTHORIZED,
                                "IDENTITY_106", "IDENTITY_203", "IDENTITY_210", "IDENTITY_224",
                                "IDENTITY_407");
                registerErrors(HttpStatus.FORBIDDEN,
                                "IDENTITY_204", "IDENTITY_206", "IDENTITY_220", "IDENTITY_225",
                                "IDENTITY_502");
                registerErrors(HttpStatus.BAD_REQUEST,
                                "IDENTITY_004", "IDENTITY_006", "IDENTITY_101", "IDENTITY_102",
                                "IDENTITY_104", "IDENTITY_105", "IDENTITY_111", "IDENTITY_202",
                                "IDENTITY_209", "IDENTITY_211", "IDENTITY_212", "IDENTITY_213",
                                "IDENTITY_222", "IDENTITY_223", "IDENTITY_226", "IDENTITY_227",
                                "IDENTITY_301", "IDENTITY_304", "IDENTITY_404", "IDENTITY_405",
                                "IDENTITY_602");
                registerErrors(HttpStatus.TOO_MANY_REQUESTS, "IDENTITY_103", "IDENTITY_107");
                registerErrors(HttpStatus.BAD_GATEWAY, "IDENTITY_406");
                setDefaultStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @Override
        protected String moduleDomain() {
                return "Identity";
        }

        @ExceptionHandler(IdentityException.class)
        public ResponseEntity<ApiResponse<Map<String, Object>>> handleIdentityException(IdentityException ex) {
                log.warn("Identity exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
                return handleException(ex);
        }
}
