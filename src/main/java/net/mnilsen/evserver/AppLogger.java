package net.mnilsen.evserver;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppLogger {

    public static final Logger LOGGER = LoggerFactory.getLogger("evserver");

    static {
        locateLogFile();
    }

    private AppLogger() {
    }

    public static void locateLogFile() {
        // Fetch the Root Logger specifically
        Logger slf4jLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        if (slf4jLogger instanceof ch.qos.logback.classic.Logger logbackRootLogger) {
            Iterator<Appender<ILoggingEvent>> it = logbackRootLogger.iteratorForAppenders();
            while (it.hasNext()) {
                Appender<ILoggingEvent> appender = it.next();
                if (appender instanceof FileAppender) {

                    // Cast down to expose file-specific methods
                    FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) appender;
                    String rawPath = fileAppender.getFile();
                    Path p = Paths.get(rawPath).normalize().toAbsolutePath();
                    AppState.setCurrentLogFile(p);
                    LOGGER.info(String.format("Logging file at '%s'", rawPath));
                }
            }
        } else {
            System.out.println("The underlying logging framework is not Logback!");
        }
    }
}
