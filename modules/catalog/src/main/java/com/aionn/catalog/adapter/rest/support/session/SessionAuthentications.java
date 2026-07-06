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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new CatalogException(CatalogErrorCode.MERCHANT_FORBIDDEN, "Authenticated principal required");
        }
        return authentication.getName();
    }
}
