package net.mnilsen.evserver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.opencsv.CSVWriter;

public final class CsvDataStore {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final Path directory;
    private volatile Path filePath;
    private volatile YearMonth activeMonth;

    public CsvDataStore(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        AppLogger.LOGGER.info("Initializing CSV store in directory: {}", directory);
        rotateToIfNeeded(YearMonth.now());
    }

    public static Path filePathFor(Path directory, YearMonth month) {
        String fileName = "readings-" + month.format(MONTH_FORMATTER) + ".csv";
        return directory.resolve(fileName);
    }

    public void write(EnvironmentalReading reading) throws IOException {
        rotateToIfNeeded(YearMonth.now());

        IOException lastException = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile(), true))) {
                    writer.writeNext(new String[]{
                        reading.timestamp().toString(),
                        String.format(Locale.US, "%.2f", reading.temperatureC()),
                        String.format(Locale.US, "%.2f", reading.pressureHpa()),
                        String.format(Locale.US, "%.2f", reading.humidityPercent()),
                        String.format(Locale.US, "%.2f", reading.gasResistanceOhms())
                    });
                }
                AppLogger.LOGGER.debug("Persisted reading to CSV at {}: {}", filePath, reading);
                return;
            } catch (IOException e) {
                lastException = e;
                if (attempt < 3) {
                    long backoffMs = 250L * attempt;
                    AppLogger.LOGGER.warn("CSV write attempt {} failed for {}. Retrying in {} ms.", attempt, filePath, backoffMs, e);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }

        AppLogger.LOGGER.error("Failed to write reading after 3 attempts to {}: {}", filePath, lastException.getMessage(), lastException);
        throw lastException;
    }

    public Path filePath() {
        return filePath;
    }

    private void rotateToIfNeeded(YearMonth month) throws IOException {
        if (activeMonth != null && month.equals(activeMonth) && Files.exists(filePath)) {
            return;
        }

        Path nextFile = filePathFor(directory, month);
        Files.createDirectories(directory);

        if (Files.notExists(nextFile)) {
            AppLogger.LOGGER.info("Creating new monthly CSV file: {}", nextFile);
            Files.writeString(nextFile, EnvironmentalReading.csvHeader() + System.lineSeparator());
        } else {
            AppLogger.LOGGER.info("Using existing monthly CSV file: {}", nextFile);
            recoverIncompleteTail(nextFile);
        }

        filePath = nextFile;
        activeMonth = month;
    }

    private static void recoverIncompleteTail(Path path) throws IOException {
        String content = Files.readString(path);
        if (content.isBlank()) {
            AppLogger.LOGGER.warn("CSV file {} is empty; recreating header.", path);
            Files.writeString(path, EnvironmentalReading.csvHeader() + System.lineSeparator());
            return;
        }

        String[] lines = content.split("\\R", -1);
        if (lines.length <= 1) {
            return;
        }

        String lastLine = lines[lines.length - 1];
        if (isCompleteCsvRecord(lastLine)) {
            return;
        }

        AppLogger.LOGGER.warn("Detected incomplete tail in CSV file {}. Removing last partial line.", path);
        StringBuilder rebuilt = new StringBuilder();
        for (int i = 0; i < lines.length - 1; i++) {
            rebuilt.append(lines[i]).append(System.lineSeparator());
        }

        Files.writeString(path, rebuilt.toString());
    }

    private static boolean isCompleteCsvRecord(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String[] values = line.split(",", -1);
        if (values.length != 5) {
            return false;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
