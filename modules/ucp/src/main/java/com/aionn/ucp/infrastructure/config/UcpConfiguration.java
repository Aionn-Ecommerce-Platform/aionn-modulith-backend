package com.aionn.ucp.infrastructure.config;

import com.aionn.ucp.adapter.rest.filter.UcpAgentAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ UcpProperties.class, UcpSessionCleanupProperties.class })
public class UcpConfiguration {

    /**
     * {@link UcpAgentAuthenticationFilter} is a {@code @Component} so the security
     * chain can inject it, but it must NOT also run as a plain servlet-container
     * filter: out of the security chain it would evaluate before the bearer filter
     * has populated {@code SecurityContextHolder}, wrongly rejecting buyer-JWT
     * requests. This registration bean disables the automatic container mapping —
     * the filter only runs where {@code ApiSecurityConfig} inserts it, after
     * {@code BearerAuthenticationFilter}.
     */
    @Bean
    public FilterRegistrationBean<UcpAgentAuthenticationFilter> ucpAgentAuthContainerRegistration(
            UcpAgentAuthenticationFilter filter) {
        FilterRegistrationBean<UcpAgentAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
