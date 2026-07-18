package com.aionn.ordering.adapter.rest.config;

import com.aionn.ordering.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.ordering.adapter.rest.support.session.CurrentMerchantIdArgumentResolver;
import com.aionn.ordering.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class OrderingRestAdapterConfig implements WebMvcConfigurer {

    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;
    private final CurrentMerchantIdArgumentResolver currentMerchantIdArgumentResolver;
    private final CurrentAdminIdArgumentResolver currentAdminIdArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
        resolvers.add(currentMerchantIdArgumentResolver);
        resolvers.add(currentAdminIdArgumentResolver);
    }
}
