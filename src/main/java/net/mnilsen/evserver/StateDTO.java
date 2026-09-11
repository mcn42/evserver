package net.mnilsen.evserver;

import java.time.LocalDateTime;

/**
 *
 * @author michaeln
 */
public class StateDTO {

    private boolean running;
    private LocalDateTime startupTime;
    private long updateCount;
    private String logFilePath;
    private String dataFilePath;

    public StateDTO() {
    }

    public StateDTO(boolean running, LocalDateTime startupTime, long updateCount, String logFilePath, String dataFilePath) {
        this.running = running;
        this.startupTime = startupTime;
        this.updateCount = updateCount;
        this.logFilePath = logFilePath;
        this.dataFilePath = dataFilePath;
    }

    public boolean isRunning() {
        return running;
    }

    public LocalDateTime getStartupTime() {
        return startupTime;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public String getDataFilePath() {
        return dataFilePath;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setStartupTime(LocalDateTime startupTime) {
        this.startupTime = startupTime;
    }

    public void setUpdateCount(long updateCount) {
        this.updateCount = updateCount;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    public void setDataFilePath(String dataFilePath) {
        this.dataFilePath = dataFilePath;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder("{ ");
        sb.append(String.format("\"startupTime\":\"%s\", ", startupTime));
        sb.append(String.format("\"running\":\"%b\", ", running));
        sb.append(String.format("\"updateCount\":\"%s\", ", updateCount));
        sb.append(String.format("\"logFilePath\":\"%s\", ", logFilePath));
        sb.append(String.format("\"dataFilePath\":\"%s\", ", dataFilePath));
        sb.append("}");
        return sb.toString();
    }
}
