package com.aionn.shipping.infrastructure.carrier;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class GhnStubServer implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, Response> responses = new ConcurrentHashMap<>();
    private final Map<String, String> receivedBodies = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> receivedHeaders = new ConcurrentHashMap<>();
    private final Map<String, Integer> hitCounts = new ConcurrentHashMap<>();

    private GhnStubServer(HttpServer server) {
        this.server = server;
    }

    static GhnStubServer start() {
        try {
            HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            GhnStubServer stub = new GhnStubServer(http);
            http.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                try (InputStream in = exchange.getRequestBody()) {
                    stub.receivedBodies.put(path, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
                Map<String, String> headers = new LinkedHashMap<>();
                exchange.getRequestHeaders()
                        .forEach((name, values) -> headers.put(name, String.join(",", values)));
                stub.receivedHeaders.put(path, headers);
                stub.hitCounts.merge(path, 1, Integer::sum);

                Response response = stub.responses.getOrDefault(path,
                        new Response(404, "{\"code\":404,\"message\":\"no stub for " + path + "\"}"));
                byte[] payload = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(response.status(), payload.length);
                exchange.getResponseBody().write(payload);
                exchange.close();
            });
            http.start();
            return stub;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to start GHN stub server", ex);
        }
    }

    GhnStubServer stub(String path, String body) {
        return stub(path, 200, body);
    }

    GhnStubServer stub(String path, int status, String body) {
        responses.put(path, new Response(status, body));
        return this;
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    String bodyReceivedAt(String path) {
        return receivedBodies.getOrDefault(path, "");
    }

    String headerReceivedAt(String path, String header) {
        return receivedHeaders.getOrDefault(path, Map.of()).get(header);
    }

    int hitsAt(String path) {
        return hitCounts.getOrDefault(path, 0);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private record Response(int status, String body) {
    }
}
