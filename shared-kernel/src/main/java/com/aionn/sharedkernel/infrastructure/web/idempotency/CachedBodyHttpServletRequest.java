package com.aionn.sharedkernel.infrastructure.web.idempotency;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request, int maxCachedBodyBytes) throws IOException {
        super(request);
        this.cachedBody = readRequestBody(request.getInputStream(), maxCachedBodyBytes);
    }

    public byte[] getCachedBody() {
        return cachedBody.clone();
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("Async read listener is not supported for cached requests");
            }

            @Override
            public int read() {
                return inputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    private static byte[] readRequestBody(InputStream inputStream, int maxCachedBodyBytes) throws IOException {
        byte[] body = inputStream.readNBytes(maxCachedBodyBytes + 1);
        if (body.length > maxCachedBodyBytes) {
            throw new IOException("Request body is too large for idempotent caching");
        }
        return body;
    }
}
