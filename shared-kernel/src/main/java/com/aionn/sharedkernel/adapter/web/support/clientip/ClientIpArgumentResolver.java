package com.aionn.sharedkernel.adapter.web.support.clientip;

import com.aionn.sharedkernel.infrastructure.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class ClientIpArgumentResolver implements HandlerMethodArgumentResolver {

    private final ClientIpResolver clientIpResolver;

    public ClientIpArgumentResolver(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ClientIp.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return null;
        }
        return clientIpResolver.resolve(request);
    }
}
