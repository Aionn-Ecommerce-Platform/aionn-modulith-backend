package com.aionn.ordering.adapter.rest.support.session;

import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.sharedkernel.integration.port.catalog.MerchantOwnershipVerifierPort;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link CurrentMerchantId} parameters from the {@code X-Merchant-Id}
 * request header and verifies that the authenticated user owns the merchant.
 *
 * <p>
 * Mirrors the Catalog/Inventory resolvers. Ordering operations operating on
 * the merchant-side (confirmPreparation, rejectByMerchant, approveReturn,
 * listForMerchant, ...) need the real Merchant aggregate id (ULID), not the
 * user principal name. The frontend (multi-shop selector) sends the chosen
 * merchant via header on every merchant-scoped request.
 * </p>
 */
@Component("orderingCurrentMerchantIdArgumentResolver")
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new OrderingException(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT,
                    "Authenticated merchant principal required");
        }
        HttpServletRequest httpRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        String merchantId = httpRequest == null ? null : httpRequest.getHeader(MERCHANT_HEADER);
        if (merchantId == null || merchantId.isBlank()) {
            throw new OrderingException(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT,
                    "Missing required header: " + MERCHANT_HEADER);
        }
        if (!merchantOwnershipVerifier.isOwnedBy(merchantId, authentication.getName())) {
            throw new OrderingException(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT,
                    "User does not own merchant " + merchantId);
        }
        return merchantId;
    }
}
