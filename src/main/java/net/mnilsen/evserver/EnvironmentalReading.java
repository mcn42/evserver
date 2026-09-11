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
                + "\"temperatureC\":" + Util.formatDouble(temperatureC) + ","
                + "\"pressureHpa\":" + Util.formatDouble(pressureHpa) + ","
                + "\"humidityPercent\":" + Util.formatDouble(humidityPercent) + ","
                + "\"gasResistanceOhms\":" + Util.formatDouble(gasResistanceOhms)
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

}
