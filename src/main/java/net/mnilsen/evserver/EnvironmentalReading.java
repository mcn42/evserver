package net.mnilsen.evserver;

import java.time.Instant;
import java.util.Locale;

public record EnvironmentalReading(
        Instant timestamp,
        double temperatureC,
        double pressureHpa,
        double humidityPercent,
        double gasResistanceOhms
) {
    public EnvironmentalReading {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public String toJson() {
        return "{"
                + "\"timestamp\":\"" + timestamp + "\","
                + "\"temperatureC\":" + formatDouble(temperatureC) + ","
                + "\"pressureHpa\":" + formatDouble(pressureHpa) + ","
                + "\"humidityPercent\":" + formatDouble(humidityPercent) + ","
                + "\"gasResistanceOhms\":" + formatDouble(gasResistanceOhms)
                + "}";
    }

    public String toCsvRow() {
        return String.format(Locale.US,
                "%s,%.2f,%.2f,%.2f,%.2f",
                timestamp,
                temperatureC,
                pressureHpa,
                humidityPercent,
                gasResistanceOhms
        );
    }

    public static String csvHeader() {
        return "timestamp,temperatureC,pressureHpa,humidityPercent,gasResistanceOhms";
    }

    private static String formatDouble(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
