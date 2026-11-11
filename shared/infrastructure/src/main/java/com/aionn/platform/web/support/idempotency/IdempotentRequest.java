package com.aionn.platform.web.support.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentRequest {

    int ttlSeconds() default 300;

    /**
     * When true the {@code Idempotency-Key} header is mandatory: a mutating
     * request without it is rejected instead of running non-idempotently.
     * Protocol contracts that require the header (e.g. UCP) set this.
     */
    boolean required() default false;
}
