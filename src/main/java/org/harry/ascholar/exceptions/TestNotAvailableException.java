package org.harry.ascholar.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TestNotAvailableException extends BusinessException {
    private final Long testId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final LocalDateTime currentTime;
    private final String reason;

    public TestNotAvailableException(Long testId) {
        super(String.format("Test %d is not currently available", testId));
        this.testId = testId;
        this.startTime = null;
        this.endTime = null;
        this.currentTime = LocalDateTime.now();
        this.reason = "NOT_AVAILABLE";
    }

    public TestNotAvailableException(Long testId, LocalDateTime startTime, LocalDateTime endTime) {
        super(String.format("Test %d is not available. Available from %s to %s",
                testId, startTime, endTime));
        this.testId = testId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentTime = LocalDateTime.now();
        this.reason = determineReason(startTime, endTime, currentTime);
    }

    public TestNotAvailableException(Long testId, String reason) {
        super(String.format("Test %d is not available: %s", testId, reason));
        this.testId = testId;
        this.startTime = null;
        this.endTime = null;
        this.currentTime = LocalDateTime.now();
        this.reason = reason;
    }

    public TestNotAvailableException(Long testId, LocalDateTime startTime, LocalDateTime endTime, Throwable cause) {
        super(String.format("Test %d is not available. Available from %s to %s",
                testId, startTime, endTime), cause);
        this.testId = testId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentTime = LocalDateTime.now();
        this.reason = determineReason(startTime, endTime, currentTime);
    }

    public Long getTestId() {
        return testId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String getErrorCode() {
        return "TEST_NOT_AVAILABLE";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    private String determineReason(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime currentTime) {
        if (startTime != null && currentTime.isBefore(startTime)) {
            return "NOT_STARTED";
        } else if (endTime != null && currentTime.isAfter(endTime)) {
            return "ENDED";
        } else {
            return "UNAVAILABLE";
        }
    }

    public boolean isNotStarted() {
        return "NOT_STARTED".equals(reason);
    }

    public boolean isEnded() {
        return "ENDED".equals(reason);
    }

    public String getDetailedMessage() {
        if (startTime != null && endTime != null) {
            return String.format("Test %d is not available. Current time: %s, Available window: %s to %s",
                    testId, currentTime, startTime, endTime);
        }
        return getMessage();
    }
}