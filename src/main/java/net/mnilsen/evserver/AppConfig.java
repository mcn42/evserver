package net.mnilsen.evserver;

public final class AppConfig {
    public static final int DEFAULT_READING_INTERVAL_SECONDS = 60;
    public static final int DEFAULT_HTTP_PORT = 8080;
    public static final String DEFAULT_DATA_DIR = "data";

    private final int readingIntervalSeconds;
    private final int httpPort;
    private final String dataDirectory;

    private AppConfig(int readingIntervalSeconds, int httpPort, String dataDirectory) {
        this.readingIntervalSeconds = readingIntervalSeconds;
        this.httpPort = httpPort;
        this.dataDirectory = dataDirectory;
    }

    public static AppConfig fromArgs(String[] args) {
        int readingIntervalSeconds = DEFAULT_READING_INTERVAL_SECONDS;
        int httpPort = DEFAULT_HTTP_PORT;
        String dataDirectory = DEFAULT_DATA_DIR;

        AppLogger.LOGGER.info("Parsing startup arguments: {}", args.length);

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--interval-seconds" -> {
                    readingIntervalSeconds = Integer.parseInt(args[++i]);
                    AppLogger.LOGGER.info("Configured reading interval seconds: {}", readingIntervalSeconds);
                }
                case "--http-port" -> {
                    httpPort = Integer.parseInt(args[++i]);
                    AppLogger.LOGGER.info("Configured HTTP port: {}", httpPort);
                }
                case "--data-dir" -> {
                    dataDirectory = args[++i];
                    AppLogger.LOGGER.info("Configured data directory: {}", dataDirectory);
                }
                case "--help", "-h" -> {
                    printUsage();
                    System.exit(0);
                }
                default -> {
                    AppLogger.LOGGER.error("Unknown startup option: {}", arg);
                    throw new IllegalArgumentException("Unknown option: " + arg);
                }
            }
        }

        return new AppConfig(readingIntervalSeconds, httpPort, dataDirectory);
    }

    public int readingIntervalSeconds() {
        return readingIntervalSeconds;
    }

    public int httpPort() {
        return httpPort;
    }

    public String dataDirectory() {
        return dataDirectory;
    }

    public static void printUsage() {
        AppLogger.LOGGER.info("Usage: java -jar evserver.jar [--interval-seconds <seconds>] [--http-port <port>] [--data-dir <path>]");
    }
}
