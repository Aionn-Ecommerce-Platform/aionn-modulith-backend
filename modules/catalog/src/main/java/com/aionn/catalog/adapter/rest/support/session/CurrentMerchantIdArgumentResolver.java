package com.aionn.catalog.adapter.rest.support.session;

import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.sharedkernel.integration.port.catalog.MerchantOwnershipVerifierPort;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentMerchantIdArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String MERCHANT_HEADER = "X-Merchant-Id";

    private final MerchantOwnershipVerifierPort merchantOwnershipVerifier;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentMerchantId.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        String ownerId = SessionAuthentications.requirePrincipalName();
        HttpServletRequest httpRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        String merchantId = httpRequest == null ? null : httpRequest.getHeader(MERCHANT_HEADER);
        if (merchantId == null || merchantId.isBlank()) {
            throw new CatalogException(CatalogErrorCode.MERCHANT_FORBIDDEN,
                    "Missing required header: " + MERCHANT_HEADER);
        }
        if (!merchantOwnershipVerifier.isOwnedBy(merchantId, ownerId)) {
            throw new CatalogException(CatalogErrorCode.MERCHANT_FORBIDDEN,
                    "User does not own merchant " + merchantId);
        }
        return merchantId;
    }
}
