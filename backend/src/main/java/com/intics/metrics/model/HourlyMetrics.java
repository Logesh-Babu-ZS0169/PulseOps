package com.intics.metrics.model;

import java.time.LocalDateTime;

public class HourlyMetrics {
    private LocalDateTime timeFrameStart;
    private LocalDateTime timeFrameEnd;
    private Long totalIngestion;
    private Long stagedInIngestion;
    private Long inProgressCount;
    private Long completedCount;
    private Long failedInProcess;
    private Long failedInIngestion;
    private Long abortedCount;

    // No-arg constructor
    public HourlyMetrics() {}

    // Constructor with all fields
    public HourlyMetrics(LocalDateTime timeFrameStart,
                         LocalDateTime timeFrameEnd,
                         Long totalIngestion,
                         Long stagedInIngestion,
                         Long inProgressCount,
                         Long completedCount,
                         Long failedInProcess,
                         Long failedInIngestion,
                         Long abortedCount) {
        this.timeFrameStart = timeFrameStart;
        this.timeFrameEnd = timeFrameEnd;
        this.totalIngestion = totalIngestion;
        this.stagedInIngestion = stagedInIngestion;
        this.inProgressCount = inProgressCount;
        this.completedCount = completedCount;
        this.failedInProcess = failedInProcess;
        this.failedInIngestion = failedInIngestion;
        this.abortedCount = abortedCount;
    }

    // Getters and Setters
    public LocalDateTime getTimeFrameStart() { return timeFrameStart; }
    public void setTimeFrameStart(LocalDateTime timeFrameStart) { this.timeFrameStart = timeFrameStart; }

    public LocalDateTime getTimeFrameEnd() { return timeFrameEnd; }
    public void setTimeFrameEnd(LocalDateTime timeFrameEnd) { this.timeFrameEnd = timeFrameEnd; }

    public Long getTotalIngestion() { return totalIngestion; }
    public void setTotalIngestion(Long totalIngestion) { this.totalIngestion = totalIngestion; }

    public Long getStagedInIngestion() { return stagedInIngestion; }
    public void setStagedInIngestion(Long stagedInIngestion) { this.stagedInIngestion = stagedInIngestion; }

    public Long getInProgressCount() { return inProgressCount; }
    public void setInProgressCount(Long inProgressCount) { this.inProgressCount = inProgressCount; }

    public Long getCompletedCount() { return completedCount; }
    public void setCompletedCount(Long completedCount) { this.completedCount = completedCount; }

    public Long getFailedInProcess() { return failedInProcess; }
    public void setFailedInProcess(Long failedInProcess) { this.failedInProcess = failedInProcess; }

    public Long getFailedInIngestion() { return failedInIngestion; }
    public void setFailedInIngestion(Long failedInIngestion) { this.failedInIngestion = failedInIngestion; }

    public Long getAbortedCount() { return abortedCount; }
    public void setAbortedCount(Long abortedCount) { this.abortedCount = abortedCount; }
}
