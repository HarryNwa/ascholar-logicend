package org.harry.ascholar.exceptions;

public class TestAttemptNotFoundException extends RuntimeException {
    public TestAttemptNotFoundException(Long attemptId) {
        super("Test attempt not found with id: " + attemptId);
    }
}
