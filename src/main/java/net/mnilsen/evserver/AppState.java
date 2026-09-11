package net.mnilsen.evserver;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author michaeln
 */
public class AppState {

    private static final AtomicLong updateCount = new AtomicLong();
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final LocalDateTime startupTime;
    private static final AtomicReference<Path> currentDataFile = new AtomicReference<>(null);
    private static final AtomicReference<Path> currentLogFile = new AtomicReference<>(null);

    static {
        startupTime = LocalDateTime.now();
    }

    public static LocalDateTime getStartupTime() {
        return startupTime;
    }

    public static long incrementUpdateCount() {
        return updateCount.incrementAndGet();
    }

    public static long getUpdateCount() {
        return updateCount.get();
    }

    public static boolean isRunning() {
        return running.get();
    }

    public static void setRunning(boolean run) {
        running.set(run);
    }

    public static Path getCurrentDataFile() {
        return currentDataFile.get();
    }

    public static Path getCurrentLogFile() {
        return currentLogFile.get();
    }

    public static void setCurrentDataFile(Path p) {
        currentDataFile.set(p);
    }

    public static void setCurrentLogFile(Path p) {
        currentLogFile.set(p);
    }

    public static synchronized StateDTO getState() {
        return new StateDTO(running.get(), startupTime, updateCount.get(), currentLogFile.get().toString(), currentDataFile.get().toString());
    }
}
