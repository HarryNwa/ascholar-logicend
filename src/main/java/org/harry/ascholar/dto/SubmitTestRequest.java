package org.harry.ascholar.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import org.harry.ascholar.utils.ValidationUtils;

import java.util.Objects;

@Builder
public record SubmitTestRequest(
        @NotNull(message = "Attempt ID is required")
        @Positive(message = "Attempt ID must be a positive number")
        Long attemptId,

        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be a positive number")
        Long userId,

        Boolean forceSubmit, // Allow forced submission even if time remains

        String submissionReason // Reason for submission (normal, technical_issue, etc.)
) {

    @JsonCreator
    public SubmitTestRequest {
        Objects.requireNonNull(attemptId, "Attempt ID cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");

        // Default values
        forceSubmit = forceSubmit != null ? forceSubmit : false;
        submissionReason = submissionReason != null ? submissionReason : "NORMAL";
    }

    public void validate() {
        ValidationUtils.validateId(attemptId, "attemptId");
        ValidationUtils.validateId(userId, "userId");

        if (submissionReason != null) {
            ValidationUtils.validateSubmissionReason(submissionReason);
        }
    }

    public boolean isForcedSubmission() {
        return Boolean.TRUE.equals(forceSubmit);
    }
}
