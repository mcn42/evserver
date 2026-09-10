package net.mnilsen.evserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class EnvServerConfigurationTest {
    @Test
    void defaultsAreSetForPiDeployment() {
        assertEquals(60, AppConfig.DEFAULT_READING_INTERVAL_SECONDS);
        assertEquals(8080, AppConfig.DEFAULT_HTTP_PORT);
        assertTrue(AppConfig.DEFAULT_DATA_DIR.endsWith("data"));
    }

    @Test
    void readingJsonIsSerializable() {
        EnvironmentalReading reading = new EnvironmentalReading(
                Instant.parse("2026-08-02T12:00:00Z"),
                22.4,
                1013.2,
                48.5,
                53640.0
        );

        String json = reading.toJson();
        assertTrue(json.contains("\"temperatureC\":22.4"));
        assertTrue(json.contains("\"timestamp\":\"2026-08-02T12:00:00Z\""));
    }

    @Test
    void csvFilesRotateByMonth() throws IOException {
        Path dataDir = Files.createTempDirectory("evserver-monthly");
        YearMonth month = YearMonth.of(2026, 8);

        Path file = CsvDataStore.filePathFor(dataDir, month);

        assertEquals(dataDir.resolve("readings-2026-08.csv"), file);
    }

    @Test
    void csvStoreReusesCurrentMonthFileAcrossRestart() throws IOException {
        Path dataDir = Files.createTempDirectory("evserver-restart");
        CsvDataStore firstStore = new CsvDataStore(dataDir);
        CsvDataStore secondStore = new CsvDataStore(dataDir);

        assertEquals(firstStore.filePath(), secondStore.filePath());
        assertTrue(firstStore.filePath().getFileName().toString().startsWith("readings-"));
        assertTrue(Files.readString(firstStore.filePath()).contains("timestamp"));
    }

    @Test
    void csvStoreTruncatesIncompleteTailOnStartup() throws IOException {
        Path dataDir = Files.createTempDirectory("evserver-tail-recovery");
        Path file = CsvDataStore.filePathFor(dataDir, YearMonth.now());

        Files.writeString(file,
                "timestamp,temperatureC,pressureHpa,humidityPercent,gasResistanceOhms\n"
                        + "2026-08-02T01:00:00Z,22.10,1013.20,48.50,45000.00\n"
                        + "2026-08-02T01:01:00Z,22.20,1013.30,48.60,"
        );

        new CsvDataStore(dataDir);

        String contents = Files.readString(file);
        assertTrue(contents.contains("2026-08-02T01:00:00Z,22.10,1013.20,48.50,45000.00"));
        assertTrue(!contents.contains("2026-08-02T01:01:00Z,22.20,1013.30,48.60,"));
    }

    @Test
    void bme680TemperatureCompensationProducesRoomTemperature() {
        int rawTemperature = 519888;
        int digT1 = 27504;
        int digT2 = 26435;
        int digT3 = -1000;

        double temperatureC = Bme680Sensor.convertTemperatureC(rawTemperature, digT1, digT2, digT3);

        assertTrue(temperatureC > 20.0 && temperatureC < 30.0,
                () -> "Expected room-temperature compensation, got " + temperatureC + "C");
    }

    @Test
    void sensorReadFallsBackToSimulationWhenHardwareUnavailable() {
        Bme680Sensor sensor = new Bme680Sensor(true);
        EnvironmentalReading reading = sensor.read();

        assertTrue(reading.temperatureC() >= -20.0);
        assertTrue(reading.humidityPercent() >= 0.0);
        assertTrue(reading.gasResistanceOhms() > 0.0);
    }
}
