package com.aionn.identity.adapter.rest.config;

import com.aionn.identity.adapter.rest.support.client.AuthClientTypeArgumentResolver;
import com.aionn.identity.adapter.rest.support.client.ClientUserAgentArgumentResolver;
import com.aionn.identity.adapter.rest.support.session.CurrentAccessTokenJtiArgumentResolver;
import com.aionn.identity.adapter.rest.support.session.CurrentSessionIdArgumentResolver;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIpArgumentResolver;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RestAdapterConfig implements WebMvcConfigurer {

	private final ClientIpArgumentResolver clientIpArgumentResolver;
	private final ClientUserAgentArgumentResolver clientUserAgentArgumentResolver;
	private final AuthClientTypeArgumentResolver authClientTypeArgumentResolver;
	private final CurrentSessionIdArgumentResolver currentSessionIdArgumentResolver;
	private final CurrentAccessTokenJtiArgumentResolver currentAccessTokenJtiArgumentResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(clientIpArgumentResolver);
		resolvers.add(clientUserAgentArgumentResolver);
		resolvers.add(authClientTypeArgumentResolver);
		resolvers.add(currentSessionIdArgumentResolver);
		resolvers.add(currentAccessTokenJtiArgumentResolver);
	}
}
