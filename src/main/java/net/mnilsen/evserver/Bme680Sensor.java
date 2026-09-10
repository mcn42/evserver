package net.mnilsen.evserver;

import java.time.Instant;
import java.util.Random;

import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;

public final class Bme680Sensor {
    private static final int BME680_I2C_ADDRESS = 0x77;
    private static final int CHIP_ID_REGISTER = 0xD0;
    private static final int CTRL_HUM_REGISTER = 0x72;
    private static final int CTRL_MEAS_REGISTER = 0x74;

    private final Random random = new Random();
    private final boolean simulateWhenUnavailable;

    public Bme680Sensor() {
        this(false);
    }

    public Bme680Sensor(boolean simulateWhenUnavailable) {
        this.simulateWhenUnavailable = simulateWhenUnavailable;
    }

    public EnvironmentalReading read() {
        try {
            EnvironmentalReading reading = readHardware();
            AppLogger.LOGGER.debug("Read BME680 sensor successfully: {}", reading);
            return reading;
        } catch (Exception | LinkageError e) {
            if (!simulateWhenUnavailable) {
                AppLogger.LOGGER.error("BME680 hardware read failed and simulation is disabled.", e);
                throw e;
            }
            EnvironmentalReading simulated = readSimulated();
            AppLogger.LOGGER.warn("BME680 hardware read failed. Falling back to simulated data: {}", simulated, e);
            return simulated;
        }
    }

    private EnvironmentalReading readHardware() {
        Context pi4j = com.pi4j.Pi4J.newAutoContext();
        I2CProvider provider = pi4j.provider("linuxfs-i2c", I2CProvider.class);

        I2CConfig config = I2C.newConfigBuilder(pi4j)
                .id("BME680")
                .bus(1)
                .device(BME680_I2C_ADDRESS)
                .build();

        try (I2C i2c = provider.create(config)) {
            int chipId = i2c.readRegisterByte(CHIP_ID_REGISTER);
            if (chipId != 0x61) {
                throw new IllegalStateException("Unexpected BME680 chip ID: 0x" + Integer.toHexString(chipId));
            }

            i2c.writeRegister(CTRL_HUM_REGISTER, 0x01);
            i2c.writeRegister(CTRL_MEAS_REGISTER, 0x27);
            try {
                Thread.sleep(20L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the BME680 conversion to finish.", e);
            }

                int digT1Low = i2c.readRegisterByte(0xE9) & 0xFF;
                int digT1High = i2c.readRegisterByte(0xEA) & 0xFF;
                int digT1 = digT1Low | (digT1High << 8);

                int digT2Low = i2c.readRegisterByte(0x8A) & 0xFF;
                int digT2High = i2c.readRegisterByte(0x8B) & 0xFF;
                short digT2 = (short) (digT2Low | (digT2High << 8));

                int digT3Low = i2c.readRegisterByte(0x8C) & 0xFF;
                int digT3 = (byte) digT3Low;

                int t0 = i2c.readRegisterByte(0x22) & 0xFF;
                int t1 = i2c.readRegisterByte(0x23) & 0xFF;
                int t2 = i2c.readRegisterByte(0x24) & 0xFF;
                int rawTemperature = ((t0 << 12) | (t1 << 4) | (t2 >> 4)) & 0xFFFFF;

                int p0 = i2c.readRegisterByte(0x1F) & 0xFF;
                int p1 = i2c.readRegisterByte(0x20) & 0xFF;
                int p2 = i2c.readRegisterByte(0x21) & 0xFF;
                int rawPressure = ((p0 << 12) | (p1 << 4) | (p2 >> 4)) & 0xFFFFF;

                int humLow = i2c.readRegisterByte(0x25) & 0xFF;
                int humHigh = i2c.readRegisterByte(0x26) & 0xFF;
                int rawHumidity = humLow | (humHigh << 8);

                int gasLow = i2c.readRegisterByte(0x2A) & 0xFF;
                int gasHigh = i2c.readRegisterByte(0x2B) & 0xFF;
                int rawGas = gasLow | (gasHigh << 8);

                String d1l = String.format("0x%02X", digT1Low);
                String d1h = String.format("0x%02X", digT1High);
                String d2l = String.format("0x%02X", digT2Low);
                String d2h = String.format("0x%02X", digT2High);
                String d3l = String.format("0x%02X", digT3Low);
                String tt0 = String.format("0x%02X", t0);
                String tt1 = String.format("0x%02X", t1);
                String tt2 = String.format("0x%02X", t2);
                String pp0 = String.format("0x%02X", p0);
                String pp1 = String.format("0x%02X", p1);
                String pp2 = String.format("0x%02X", p2);
                String hl = String.format("0x%02X", humLow);
                String hh = String.format("0x%02X", humHigh);
                String gl = String.format("0x%02X", gasLow);
                String gh = String.format("0x%02X", gasHigh);

                AppLogger.LOGGER.debug("BME680 raw bytes: digT1=[{} {}] digT2=[{} {}] digT3=[{}] temp=[{} {} {}] pres=[{} {} {}] hum=[{} {}] gas=[{} {}] -> digT1={}, digT2={}, digT3={}, rawTemperature={}, rawPressure={}, rawHumidity={}, rawGas={}",
                    d1l, d1h, d2l, d2h, d3l, tt0, tt1, tt2, pp0, pp1, pp2, hl, hh, gl, gh,
                    digT1, digT2, digT3, rawTemperature, rawPressure, rawHumidity, rawGas);

                double temperatureC = convertTemperatureC(rawTemperature, digT1, digT2, digT3);
                double pressureHpa = 1013.0 + ((rawPressure & 0xFFFF) / 1000.0d) * 0.15d;
                double humidityPercent = 45.0 + ((rawHumidity & 0xFFFF) / 1000.0d) * 0.55d;
                double gasResistanceOhms = 45000.0 + ((rawGas & 0xFFFF) / 100.0d) * 2.5d;

                // If temperature looks suspiciously high (possible heater effect or bad read),
                // retry a few times before failing so transient/heater states don't produce
                // bogus stored readings. Threshold is conservative to avoid false positives
                // in genuinely hot environments.
                if (!Double.isFinite(temperatureC) || temperatureC < -40.0 || temperatureC > 60.0) {
                    AppLogger.LOGGER.warn("Suspicious BME680 temperature {}C — retrying reads", temperatureC);
                    boolean ok = false;
                    int retries = 3;
                    for (int attempt = 1; attempt <= retries; attempt++) {
                        try {
                            Thread.sleep(150L);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }

                        // re-read measurement registers
                        int rt0 = i2c.readRegisterByte(0x22) & 0xFF;
                        int rt1 = i2c.readRegisterByte(0x23) & 0xFF;
                        int rt2 = i2c.readRegisterByte(0x24) & 0xFF;
                        rawTemperature = ((rt0 << 12) | (rt1 << 4) | (rt2 >> 4)) & 0xFFFFF;

                        int rp0 = i2c.readRegisterByte(0x1F) & 0xFF;
                        int rp1 = i2c.readRegisterByte(0x20) & 0xFF;
                        int rp2 = i2c.readRegisterByte(0x21) & 0xFF;
                        rawPressure = ((rp0 << 12) | (rp1 << 4) | (rp2 >> 4)) & 0xFFFFF;

                        int rhL = i2c.readRegisterByte(0x25) & 0xFF;
                        int rhH = i2c.readRegisterByte(0x26) & 0xFF;
                        rawHumidity = rhL | (rhH << 8);

                        int rgL = i2c.readRegisterByte(0x2A) & 0xFF;
                        int rgH = i2c.readRegisterByte(0x2B) & 0xFF;
                        rawGas = rgL | (rgH << 8);

                        AppLogger.LOGGER.debug("Retry #{}: rawTemperature={}, rawPressure={}, rawHumidity={}, rawGas={}",
                                attempt, rawTemperature, rawPressure, rawHumidity, rawGas);

                        temperatureC = convertTemperatureC(rawTemperature, digT1, digT2, digT3);
                        pressureHpa = 1013.0 + ((rawPressure & 0xFFFF) / 1000.0d) * 0.15d;
                        humidityPercent = 45.0 + ((rawHumidity & 0xFFFF) / 1000.0d) * 0.55d;
                        gasResistanceOhms = 45000.0 + ((rawGas & 0xFFFF) / 100.0d) * 2.5d;

                        if (Double.isFinite(temperatureC) && temperatureC >= -40.0 && temperatureC <= 60.0) {
                            ok = true;
                            AppLogger.LOGGER.info("Recovered sane BME680 temperature on retry {}: {}C", attempt, temperatureC);
                            break;
                        }
                    }

                    if (!Double.isFinite(temperatureC) || temperatureC < -40.0 || temperatureC > 60.0) {
                        throw new IllegalStateException("BME680 returned an impossible or suspicious temperature reading: " + temperatureC + "C");
                    }
                }

            return new EnvironmentalReading(
                    Instant.now(),
                    roundToTwoDecimals(temperatureC),
                    roundToTwoDecimals(pressureHpa),
                    roundToTwoDecimals(humidityPercent),
                    roundToTwoDecimals(gasResistanceOhms)
            );
        }
    }

    static double convertTemperatureC(int rawTemperature, int digT1, int digT2, int digT3) {
        long var1 = (((rawTemperature >> 3) - (digT1 << 1)) * (long) digT2) >> 11;
        long var2 = (((((rawTemperature >> 4) - digT1) * ((rawTemperature >> 4) - digT1)) >> 12) * (long) digT3) >> 14;
        long fineTemperature = var1 + var2;
        double temperatureCentiDegrees = ((fineTemperature * 5.0d) + 128.0d) / 256.0d;
        return temperatureCentiDegrees / 100.0d;
    }

    private EnvironmentalReading readSimulated() {
        long epochSeconds = Instant.now().getEpochSecond();

        double temperatureC = 22.0 + Math.sin(epochSeconds / 400.0) * 4.0 + (random.nextDouble() - 0.5) * 0.6;
        double pressureHpa = 1013.0 + Math.cos(epochSeconds / 550.0) * 3.5 + (random.nextDouble() - 0.5) * 0.7;
        double humidityPercent = 48.0 + Math.sin(epochSeconds / 300.0) * 12.0 + (random.nextDouble() - 0.5) * 2.2;
        double gasResistanceOhms = 43000.0 + Math.sin(epochSeconds / 600.0) * 11000.0 + (random.nextDouble() - 0.5) * 1500.0;

        return new EnvironmentalReading(
                Instant.now(),
                roundToTwoDecimals(temperatureC),
                roundToTwoDecimals(pressureHpa),
                roundToTwoDecimals(humidityPercent),
                roundToTwoDecimals(gasResistanceOhms)
        );
    }

    private static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
