package org.harry.ascholar.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Duration;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TestTimeExpiredException extends BusinessException {
    private final Long attemptId;
    private final Duration allowedDuration;
    private final Duration actualDuration;

    // ✅ ADD: Constructor that matches your service call
    public TestTimeExpiredException(Long attemptId, Duration allowedDuration, Duration actualDuration) {
        super(String.format("Test time expired for attempt %d. Allowed: %s, Actual: %s",
                attemptId, formatDuration(allowedDuration), formatDuration(actualDuration)));
        this.attemptId = attemptId;
        this.allowedDuration = allowedDuration;
        this.actualDuration = actualDuration;
    }

    // ✅ KEEP: Your existing constructor for backward compatibility
    public TestTimeExpiredException(String message) {
        super(message);
        this.attemptId = null;
        this.allowedDuration = null;
        this.actualDuration = null;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public Duration getAllowedDuration() {
        return allowedDuration;
    }

    public Duration getActualDuration() {
        return actualDuration;
    }

    private static String formatDuration(Duration duration) {
        if (duration == null) return "N/A";
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        return String.format("%d min %d sec", minutes, seconds);
    }
}