package org.harry.ascholar.dto;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Builder;
import org.harry.ascholar.utils.ValidationUtils;
import org.harry.ascholar.utils.SanitizationUtils;

import java.util.Objects;

@Builder
public record SubmitAnswerRequest(
        @NotNull(message = "Attempt ID is required")
        @Positive(message = "Attempt ID must be a positive number")
        Long attemptId,

        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be a positive number")
        Long userId,

        @NotNull(message = "Question ID is required")
        @Positive(message = "Question ID must be a positive number")
        Long questionId,

        @NotBlank(message = "Answer cannot be blank")
        @Size(min = 1, max = 10000, message = "Answer must be between 1 and 10,000 characters")
        String answer,

        @NotNull(message = "Time spent is required")
        @Min(value = 0, message = "Time spent cannot be negative")
        @Max(value = 3600, message = "Time spent cannot exceed 3600 seconds (1 hour)")
        Integer timeSpentSeconds,

        @Pattern(regexp = "^[A-Za-z0-9_-]{0,50}$", message = "Question type must be alphanumeric with underscores or hyphens")
        String questionType,

        @Min(value = 0, message = "Question points cannot be negative")
        @Max(value = 100, message = "Question points cannot exceed 100")
        Integer questionPoints
) {

    @JsonCreator
    public SubmitAnswerRequest {
        // Constructor validation - executed after Jackson deserialization but before field assignment
        Objects.requireNonNull(attemptId, "Attempt ID cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(questionId, "Question ID cannot be null");
        Objects.requireNonNull(answer, "Answer cannot be null");
        Objects.requireNonNull(timeSpentSeconds, "Time spent cannot be null");

        // Sanitize inputs
        answer = SanitizationUtils.sanitizeHtml(answer);
        answer = SanitizationUtils.trimWhitespace(answer);

        // Default values for optional fields
        questionType = questionType != null ? questionType : "MULTIPLE_CHOICE";
        questionPoints = questionPoints != null ? questionPoints : 1;
    }

    /**
     * Comprehensive validation method for business logic
     * @throws org.harry.ascholar.exceptions.ValidationException if validation fails
     */
    public void validate() {
        ValidationUtils.validateId(attemptId, "attemptId");
        ValidationUtils.validateId(userId, "userId");
        ValidationUtils.validateId(questionId, "questionId");
        ValidationUtils.validateString(answer, "answer", 1, 10000);
        ValidationUtils.validateRange(timeSpentSeconds, "timeSpentSeconds", 0, 3600);

        if (questionType != null) {
            ValidationUtils.validateQuestionType(questionType);
        }

        if (questionPoints != null) {
            ValidationUtils.validateRange(questionPoints, "questionPoints", 0, 100);
        }

        // Additional business logic validation
        validateAnswerContent();
    }

    /**
     * Validate answer content based on question type
     */
    private void validateAnswerContent() {
        if (questionType != null) {
            switch (questionType.toUpperCase()) {
                case "MULTIPLE_CHOICE":
                    validateMultipleChoiceAnswer();
                    break;
                case "TRUE_FALSE":
                    validateTrueFalseAnswer();
                    break;
                case "SHORT_ANSWER":
                    validateShortAnswer();
                    break;
                case "ESSAY":
                    validateEssayAnswer();
                    break;
                case "NUMERIC":
                    validateNumericAnswer();
                    break;
                default:
                    // Unknown question type, use generic validation
                    validateGenericAnswer();
            }
        } else {
            validateGenericAnswer();
        }
    }

    private void validateMultipleChoiceAnswer() {
        if (answer.length() > 10) {
            throw new IllegalArgumentException("Multiple choice answers cannot exceed 10 characters");
        }
        if (!answer.matches("^[A-Za-z0-9]{1,10}$")) {
            throw new IllegalArgumentException("Multiple choice answers must be alphanumeric");
        }
    }

    private void validateTrueFalseAnswer() {
        if (!answer.equalsIgnoreCase("true") && !answer.equalsIgnoreCase("false") &&
                !answer.equalsIgnoreCase("t") && !answer.equalsIgnoreCase("f")) {
            throw new IllegalArgumentException("True/false answers must be 'true', 'false', 't', or 'f'");
        }
    }

    private void validateShortAnswer() {
        if (answer.length() > 500) {
            throw new IllegalArgumentException("Short answers cannot exceed 500 characters");
        }
    }

    private void validateEssayAnswer() {
        if (answer.length() < 50) {
            throw new IllegalArgumentException("Essay answers must be at least 50 characters");
        }
        if (answer.length() > 10000) {
            throw new IllegalArgumentException("Essay answers cannot exceed 10,000 characters");
        }
    }

    private void validateNumericAnswer() {
        try {
            Double.parseDouble(answer);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Numeric answers must be valid numbers");
        }
    }

    private void validateGenericAnswer() {
        // Basic content validation for unknown question types
        if (answer.trim().isEmpty()) {
            throw new IllegalArgumentException("Answer cannot be empty or whitespace only");
        }

        // Check for suspicious patterns (potential injection attempts)
        if (SanitizationUtils.containsSuspiciousPatterns(answer)) {
            throw new IllegalArgumentException("Answer contains suspicious content");
        }
    }

    /**
     * Normalize the answer based on question type
     */
    public String getNormalizedAnswer() {
        if (questionType == null) {
            return answer.trim();
        }

        return switch (questionType.toUpperCase()) {
            case "MULTIPLE_CHOICE" -> answer.trim().toUpperCase();
            case "TRUE_FALSE" -> normalizeTrueFalseAnswer();
            case "NUMERIC" -> normalizeNumericAnswer();
            default -> answer.trim();
        };
    }

    private String normalizeTrueFalseAnswer() {
        String normalized = answer.trim().toLowerCase();
        if (normalized.equals("t") || normalized.equals("true")) {
            return "TRUE";
        } else if (normalized.equals("f") || normalized.equals("false")) {
            return "FALSE";
        }
        return normalized.toUpperCase();
    }

    private String normalizeNumericAnswer() {
        try {
            double value = Double.parseDouble(answer.trim());
            // Remove unnecessary decimal places for whole numbers
            if (value == (long) value) {
                return String.valueOf((long) value);
            }
            // Limit to 4 decimal places
            return String.format("%.4f", value).replaceAll("0*$", "").replaceAll("\\.$", "");
        } catch (NumberFormatException e) {
            return answer.trim();
        }
    }

    /**
     * Create a sanitized copy with sensitive data removed (for logging)
     */
    public SubmitAnswerRequest sanitizedForLogging() {
        return new SubmitAnswerRequest(
                attemptId,
                userId,
                questionId,
                "[REDACTED]", // Don't log actual answers
                timeSpentSeconds,
                questionType,
                questionPoints
        );
    }

    /**
     * Builder with additional validation
     */
    public static class SubmitAnswerRequestBuilder {
        public SubmitAnswerRequest build() {
            SubmitAnswerRequest request = new SubmitAnswerRequest(
                    attemptId, userId, questionId, answer, timeSpentSeconds, questionType, questionPoints
            );
            request.validate();
            return request;
        }
    }
}
