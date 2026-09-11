package net.mnilsen.evserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class EvServerApplication {
    private EvServerApplication() {
    }

    public static void main(String[] args) throws Exception {
        AppLogger.LOGGER.info(String.format("Starting EV server application at %s...", AppState.getStartupTime()));
        AppConfig config = AppConfig.fromArgs(args);

        Path dataDir = Path.of(config.dataDirectory());
        AppLogger.LOGGER.info("Using data directory: {}", dataDir);

        CsvDataStore dataStore = new CsvDataStore(dataDir);
        AtomicReference<EnvironmentalReading> latestReading = new AtomicReference<>();
        Bme680Sensor sensor = new Bme680Sensor(true);

        try (JsonHttpServer httpServer = new JsonHttpServer(config.httpPort(), latestReading)) {
            httpServer.start();
            AppLogger.LOGGER.info("HTTP server started on port {}", config.httpPort());
            AppLogger.LOGGER.info("CSV output file: {}", dataStore.filePath());
            Path p = dataStore.filePath().normalize().toAbsolutePath();
            AppState.setCurrentDataFile(p);

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "evserver-scheduler");
                thread.setDaemon(true);
                return thread;
            });
            
            AppState.setRunning(true);
            
            Runnable task = () -> {
                try {
                    EnvironmentalReading reading = sensor.read();
                    latestReading.set(reading);
                    dataStore.write(reading);
                    AppLogger.LOGGER.info("Stored sensor reading: {}", reading);
                } catch (IOException e) {
                    AppLogger.LOGGER.error("Failed to store sensor reading to CSV: {}", e.getMessage(), e);
                } catch (Exception e) {
                    AppLogger.LOGGER.error("Unexpected sensor read failure: {}", e.getMessage(), e);
                }
            };

            scheduler.scheduleAtFixedRate(task, 0, config.readingIntervalSeconds(), TimeUnit.SECONDS);
            AppLogger.LOGGER.info("Collecting readings every {} seconds.", config.readingIntervalSeconds());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                AppLogger.LOGGER.info("Shutdown requested. Stopping scheduler...");
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    AppLogger.LOGGER.warn("Interrupted while shutting down scheduler.");
                }
            }));

            AppLogger.LOGGER.info("Application is running. Waiting for shutdown signal...");
            Thread.currentThread().join();
        }
    }
}
