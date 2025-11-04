package org.harry.ascholar.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateTestAttemptException extends BusinessException {
    private final Long testId;
    private final Long userId;

    public DuplicateTestAttemptException(Long testId, Long userId) {
        super(String.format("User %d already has an active attempt for test %d", userId, testId));
        this.testId = testId;
        this.userId = userId;
    }

    public DuplicateTestAttemptException(Long testId, Long userId, Throwable cause) {
        super(String.format("User %d already has an active attempt for test %d", userId, testId), cause);
        this.testId = testId;
        this.userId = userId;
    }

    public Long getTestId() {
        return testId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getErrorCode() {
        return "DUPLICATE_TEST_ATTEMPT";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}