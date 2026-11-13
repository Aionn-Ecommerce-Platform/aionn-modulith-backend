package com.aionn.catalog.adapter.rest.support.session;

import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

final class SessionAuthentications {

    private SessionAuthentications() {
    }

    static String requirePrincipalName() {
        String principal = optionalPrincipalName();
        if (principal == null) {
            throw new CatalogException(CatalogErrorCode.MERCHANT_FORBIDDEN, "Authenticated principal required");
        }
        return principal;
    }

    static String optionalPrincipalName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }
}
