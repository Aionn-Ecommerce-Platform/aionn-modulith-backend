package com.aionn.identity.infrastructure.registration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRegistrationRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RedisRegistrationRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RedisRegistrationRateLimiter(redisTemplate);
    }

    private static RedisScript<Long> anyScript() {
        return ArgumentMatchers.any();
    }

    @Test
    void checkAllowsRequestWhenScriptReturnsOne() {
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.captor();
        when(redisTemplate.execute(anyScript(), keysCaptor.capture(),
                any(), any(), any(), any(), any())).thenReturn(1L);

        boolean allowed = rateLimiter.check("ip", "1.2.3.4", 3, 300);

        assertThat(allowed).isTrue();
        assertThat(keysCaptor.getValue()).containsExactly("identity:ratelimit:ip:1.2.3.4");
    }

    @Test
    void checkDeniesRequestWhenScriptReturnsZero() {
        when(redisTemplate.execute(anyScript(), anyList(),
                any(), any(), any(), any(), any())).thenReturn(0L);

        assertThat(rateLimiter.check("ip", "1.2.3.4", 3, 300)).isFalse();
    }

    @Test
    void checkDeniesRequestWhenScriptReturnsNull() {
        when(redisTemplate.execute(anyScript(), anyList(),
                any(), any(), any(), any(), any())).thenReturn(null);

        assertThat(rateLimiter.check("ip", "1.2.3.4", 3, 300)).isFalse();
    }

    @Test
    void checkAllowsAndSkipsRedisWhenKeyBlank() {
        assertThat(rateLimiter.check("ip", "  ", 3, 300)).isTrue();
        assertThat(rateLimiter.check("ip", null, 3, 300)).isTrue();

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void checkPassesExpectedScriptArguments() {
        ArgumentCaptor<Object> arg1 = ArgumentCaptor.captor();
        ArgumentCaptor<Object> arg2 = ArgumentCaptor.captor();
        ArgumentCaptor<Object> arg3 = ArgumentCaptor.captor();
        ArgumentCaptor<Object> arg4 = ArgumentCaptor.captor();
        ArgumentCaptor<Object> arg5 = ArgumentCaptor.captor();
        when(redisTemplate.execute(anyScript(), anyList(),
                arg1.capture(), arg2.capture(), arg3.capture(), arg4.capture(), arg5.capture()))
                .thenReturn(1L);

        rateLimiter.check("phone", "+8490", 5, 60);

        assertThat(arg3.getValue()).isEqualTo("5");
        assertThat(arg5.getValue()).isEqualTo(Long.toString(60L * 2));
    }
}
