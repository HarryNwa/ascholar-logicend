package org.harry.ascholar.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class StudentProfileNotFoundException extends ResourceNotFoundException {
    private final Long userId;

    public StudentProfileNotFoundException(Long userId) {
        super("StudentProfile", "userId", userId);
        this.userId = userId;
    }

    public StudentProfileNotFoundException(Long userId, Throwable cause) {
        super("StudentProfile", "userId", userId, cause);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getErrorCode() {
        return "STUDENT_PROFILE_NOT_FOUND";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getDetailedMessage() {
        return String.format("Student profile not found for user ID: %d. " +
                "Please complete your profile before attempting tests.", userId);
    }
}