# EV Server

EV Server is a small Java 25 Maven application that reads environmental data from a Bosch BME680 sensor, writes readings to a monthly CSV file, and exposes the latest reading over HTTP as JSON.

## Features

- Raspberry Pi 4 compatible design
- Reads from BME680 over I2C when hardware is available
- Falls back to a simulated reading if the sensor or native libraries are not available
- Stores readings in monthly CSV files such as `readings-2026-08.csv`
- Exposes `/readings` and `/health` over an HTTP server
- Survives app restarts by reusing the current month file and trimming incomplete tails on startup

## Build

From the project root:

```bash
mvn package
```

This produces the application JAR and a runtime dependency folder:

```bash
target/evserver.jar
target/lib/
```

## Raspberry Pi deployment

### Quick checklist

- [ ] Build the JAR on the development machine: `mvn package`
- [ ] Copy the JAR and unit file to the Pi: `scp ...`
- [ ] Create the install folders: `/opt/evserver` and `/var/lib/evserver/data`
- [ ] Install the JAR and systemd unit on the Pi
- [ ] Reload systemd and start the service
- [ ] Confirm the status is active with `systemctl status evserver`
- [ ] Check HTTP responses: `curl http://localhost:8080/health` and `curl http://localhost:8080/readings`

### 1. Copy the files to the Pi

From your development machine, copy the app JAR, dependency lib folder, and service file to the Pi:

```bash
scp target/evserver.jar pi@raspberrypi:/tmp/
scp -r target/lib pi@raspberrypi:/tmp/
scp evserver.service pi@raspberrypi:/tmp/
```

### 2. Install on the Pi

Run this on the Pi:

```bash
sudo install -d /opt/evserver /var/lib/evserver/data
sudo cp /tmp/evserver.jar /opt/evserver/
sudo cp -a /tmp/lib /opt/evserver/
sudo cp /tmp/evserver.service /etc/systemd/system/evserver.service
sudo systemctl daemon-reload
sudo systemctl enable --now evserver
```

### 3. Verify it is running

```bash
sudo systemctl status evserver
curl http://localhost:8080/health
curl http://localhost:8080/readings
```

## Command line options

```bash
java -jar /opt/evserver/evserver.jar --help
```

Available arguments:

- `--interval-seconds <seconds>`
- `--http-port <port>`
- `--data-dir <path>`

Example:

```bash
java -jar /opt/evserver/evserver.jar \
  --interval-seconds 60 \
  --http-port 8080 \
  --data-dir /var/lib/evserver/data
```

## CSV output

The app writes one monthly file in the configured data directory:

```text
/var/lib/evserver/data/readings-2026-08.csv
```

The file contains:

```csv
timestamp,temperatureC,pressureHpa,humidityPercent,gasResistanceOhms
```

## Notes

- The service is intended to run under `systemd` on Raspberry Pi OS.
- If the sensor or Pi4J native libraries are unavailable, the app continues in simulated mode so the service stays alive.
- The app reuses the active month file and removes incomplete trailing rows after a reboot or crash.
