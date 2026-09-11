package net.mnilsen.evserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;

public final class JsonHttpServer implements AutoCloseable {
    private final HttpServer httpServer;
    private final AtomicReference<EnvironmentalReading> latestReading;

    public JsonHttpServer(int port, AtomicReference<EnvironmentalReading> latestReading) throws IOException {
        this.latestReading = latestReading;
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        AppLogger.LOGGER.info("Configuring HTTP endpoints on port {}", port);

        this.httpServer.createContext("/readings", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                AppLogger.LOGGER.warn("Rejected non-GET request to /readings: {}", exchange.getRequestMethod());
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            EnvironmentalReading reading = latestReading.get();
            String responseBody = reading == null ? "{\"status\":\"waiting for first reading\"}" : reading.toJson();
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);

            AppLogger.LOGGER.debug("Serving /readings response: {}", responseBody);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(responseBytes);
            }
        });

        this.httpServer.createContext("/health", exchange -> {
            String response = AppState.getState().toJson();
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            AppLogger.LOGGER.debug("Serving /health response");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(responseBytes);
            }
        });

        this.httpServer.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        AppLogger.LOGGER.info("Starting embedded HTTP server");
        httpServer.start();
    }

    @Override
    public void close() {
        AppLogger.LOGGER.info("Stopping embedded HTTP server");
        httpServer.stop(0);
    }
}
