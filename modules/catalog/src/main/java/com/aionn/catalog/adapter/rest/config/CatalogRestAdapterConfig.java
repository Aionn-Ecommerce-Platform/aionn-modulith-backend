package com.aionn.catalog.adapter.rest.config;

import com.aionn.catalog.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.catalog.adapter.rest.support.session.CurrentMerchantIdArgumentResolver;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerIdArgumentResolver;
import com.aionn.catalog.adapter.rest.support.session.OptionalOwnerIdArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CatalogRestAdapterConfig implements WebMvcConfigurer {

    private final CurrentMerchantIdArgumentResolver currentMerchantIdArgumentResolver;
    private final CurrentOwnerIdArgumentResolver currentOwnerIdArgumentResolver;
    private final CurrentAdminIdArgumentResolver currentAdminIdArgumentResolver;
    private final OptionalOwnerIdArgumentResolver optionalOwnerIdArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMerchantIdArgumentResolver);
        resolvers.add(currentOwnerIdArgumentResolver);
        resolvers.add(currentAdminIdArgumentResolver);
        resolvers.add(optionalOwnerIdArgumentResolver);
    }
}
