package com.aionn.sharedkernel.adapter.web.support.versioning;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiVersionRequestCondition implements RequestCondition<ApiVersionRequestCondition> {

    private static final Pattern VERSION_PREFIX_PATTERN = Pattern.compile("/v(\\d+)/");
    private static final Pattern ACCEPT_HEADER_PATTERN = Pattern.compile("application/vnd\\.aionn\\.v(\\d+)\\+json");

    private final int version;

    public ApiVersionRequestCondition(int version) {
        this.version = version;
    }

    @Override
    public ApiVersionRequestCondition combine(ApiVersionRequestCondition other) {
        return new ApiVersionRequestCondition(other.version);
    }

    @Override
    public ApiVersionRequestCondition getMatchingCondition(HttpServletRequest request) {
        int requestVersion = extractVersion(request);
        if (requestVersion == this.version) {
            return this;
        }
        return null;
    }

    @Override
    public int compareTo(ApiVersionRequestCondition other, HttpServletRequest request) {
        return Integer.compare(other.version, this.version);
    }

    private int extractVersion(HttpServletRequest request) {
        String path = request.getRequestURI();
        Matcher pathMatcher = VERSION_PREFIX_PATTERN.matcher(path);
        if (pathMatcher.find()) {
            return Integer.parseInt(pathMatcher.group(1));
        }

        Integer requestParamVersion = parseVersion(request.getParameter("version"));
        if (requestParamVersion != null) {
            return requestParamVersion;
        }

        Integer headerVersion = parseVersion(request.getHeader("X-API-Version"));
        if (headerVersion != null) {
            return headerVersion;
        }

        String accept = request.getHeader("Accept");
        if (accept != null) {
            Matcher acceptMatcher = ACCEPT_HEADER_PATTERN.matcher(accept);
            if (acceptMatcher.find()) {
                return Integer.parseInt(acceptMatcher.group(1));
            }
        }

        return 1;
    }

    public int getVersion() {
        return version;
    }

    private Integer parseVersion(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
