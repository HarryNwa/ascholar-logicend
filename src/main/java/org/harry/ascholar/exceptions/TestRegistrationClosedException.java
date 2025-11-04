package org.harry.ascholar.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TestRegistrationClosedException extends BusinessException {
    private final Long testId;
    private final LocalDateTime registrationDeadline;
    private final LocalDateTime currentTime;

    public TestRegistrationClosedException(Long testId) {
        super(String.format("Registration for test %d is closed", testId));
        this.testId = testId;
        this.registrationDeadline = null;
        this.currentTime = LocalDateTime.now();
    }

    public TestRegistrationClosedException(Long testId, LocalDateTime registrationDeadline) {
        super(String.format("Registration for test %d closed at %s", testId, registrationDeadline));
        this.testId = testId;
        this.registrationDeadline = registrationDeadline;
        this.currentTime = LocalDateTime.now();
    }

    public TestRegistrationClosedException(Long testId, LocalDateTime registrationDeadline, Throwable cause) {
        super(String.format("Registration for test %d closed at %s", testId, registrationDeadline), cause);
        this.testId = testId;
        this.registrationDeadline = registrationDeadline;
        this.currentTime = LocalDateTime.now();
    }

    public Long getTestId() {
        return testId;
    }

    public LocalDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    @Override
    public String getErrorCode() {
        return "TEST_REGISTRATION_CLOSED";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    public String getDetailedMessage() {
        if (registrationDeadline != null) {
            return String.format("Test registration closed. Deadline was: %s, Current time: %s",
                    registrationDeadline, currentTime);
        }
        return getMessage();
    }
}