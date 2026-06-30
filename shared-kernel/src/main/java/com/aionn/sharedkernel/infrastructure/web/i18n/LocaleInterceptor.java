package com.aionn.sharedkernel.infrastructure.web.i18n;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;

@Component
public class LocaleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String localeHeader = request.getHeader("Accept-Language");
        if (localeHeader != null && !localeHeader.isBlank()) {
            // Parse language tag (handle list form like en-US,en;q=0.9)
            String languageTag = localeHeader.split(",")[0].trim();
            LocaleContextHolder.setLocale(Locale.forLanguageTag(languageTag));
        } else {
            // Default locale
            LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LocaleContextHolder.resetLocaleContext();
    }
}
