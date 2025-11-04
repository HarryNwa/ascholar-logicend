package org.harry.ascholar.utils;

import org.harry.ascholar.exceptions.ValidationException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ValidationUtils {
    // Existing patterns from your code
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[a-zA-Z\\s'-]{2,50}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9]{10,15}$"
    );
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$"
    );
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_-]{3,20}$"
    );

    // New patterns for test system
    private static final Pattern ID_PATTERN = Pattern.compile("^[1-9]\\d*$");
    private static final Pattern QUESTION_TYPE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    private static final Pattern USER_AGENT_PATTERN = Pattern.compile("^[\\x20-\\x7E]{0,500}$"); // Printable ASCII chars

    // Validation sets
    private static final Set<String> VALID_QUESTION_TYPES = Set.of(
            "MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER", "ESSAY", "NUMERIC", "MATCHING", "FILL_BLANK"
    );

    private static final Set<String> VALID_SUBMISSION_REASONS = Set.of(
            "NORMAL", "TIME_EXPIRED", "TECHNICAL_ISSUE", "EMERGENCY", "AUTO_SUBMIT"
    );

    private static final Set<String> VALID_TEST_STATUSES = Set.of(
            "REGISTERED", "IN_PROGRESS", "COMPLETED", "AUTO_SUBMITTED", "CANCELLED"
    );

    // ===== EXISTING VALIDATION METHODS (KEEP AS-IS) =====

    public static boolean isBlank(String value){
        return value == null || value.trim().isEmpty();
    }

    public static boolean isNotBlank(String value){
        return !isBlank(value);
    }

    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    public static boolean isValidEmail(String email){
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone){
        return isNotBlank(phone) && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidName(String name){
        return isNotBlank(name) && NAME_PATTERN.matcher(name.trim()).matches();
    }

    public static boolean isValidUrl(String url) {
        return isNotBlank(url) && URL_PATTERN.matcher(url).matches();
    }

    public static void validateUrl(String url, String fieldName) {
        if (isNotBlank(url) && !isValidUrl(url)) {
            throw new ValidationException("Invalid " + fieldName + " URL format: " + url);
        }
    }

    public static boolean isValidPassword(String password) {
        return isNotBlank(password) && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isValidUsername(String username) {
        return isNotBlank(username) && USERNAME_PATTERN.matcher(username).matches();
    }

    // ===== EXISTING VALIDATION METHODS (THROW EXCEPTIONS) =====

    public static void validateNotBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new ValidationException(fieldName + " cannot be blank");
        }
    }

    public static void validateNotNull(Object object, String fieldName) {
        if (object == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }
    }

    public static void validateEmail(String email) {
        if (!isValidEmail(email)) {
            throw new ValidationException("Invalid email format. Must be a valid email address");
        }
    }

    public static void validateName(String name, String fieldName) {
        if (!isValidName(name)) {
            throw new ValidationException(
                    "Invalid " + fieldName + " format. Must contain only letters, spaces, hyphens, " +
                            "and apostrophes (2-100 characters)"
            );
        }
    }

    public static void validatePhone(String phone) {
        if (!isValidPhone(phone)) {
            throw new ValidationException(
                    "Invalid phone number format. Must be 10-15 digits with optional + prefix"
            );
        }
    }

    public static void validatePassword(String password) {
        if (!isValidPassword(password)) {
            throw new ValidationException(
                    "Password must be at least 8 characters long and contain: " +
                            "uppercase letter, lowercase letter, number, and special character"
            );
        }
    }

    public static void validateUsername(String username) {
        if (!isValidUsername(username)) {
            throw new ValidationException(
                    "Username must be 3-50 characters and can only contain: " +
                            "letters, numbers, dots, hyphens, and underscores"
            );
        }
    }

    // ===== EXISTING LENGTH VALIDATION =====

    public static void validateMinLength(String value, int minLength, String fieldName) {
        if (value != null && value.length() < minLength) {
            throw new ValidationException(
                    fieldName + " must be at least " + minLength + " characters long"
            );
        }
    }

    public static void validateMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(
                    fieldName + " cannot exceed " + maxLength + " characters"
            );
        }
    }

    public static void validateLength(String value, int minLength, int maxLength, String fieldName) {
        if (value != null) {
            if (value.length() < minLength) {
                throw new ValidationException(
                        fieldName + " must be at least " + minLength + " characters long"
                );
            }
            if (value.length() > maxLength) {
                throw new ValidationException(
                        fieldName + " cannot exceed " + maxLength + " characters"
                );
            }
        }
    }

    // ===== EXISTING NUMBER VALIDATION =====

    public static void validatePositiveNumber(Number number, String fieldName) {
        if (number != null && number.doubleValue() <= 0) {
            throw new ValidationException(fieldName + " must be a positive number");
        }
    }

    public static void validateNonNegativeNumber(Number number, String fieldName) {
        if (number != null && number.doubleValue() < 0) {
            throw new ValidationException(fieldName + " cannot be negative");
        }
    }

    public static void validateNumberRange(Number number, Number min, Number max, String fieldName) {
        if (number != null) {
            double value = number.doubleValue();
            if (min != null && value < min.doubleValue()) {
                throw new ValidationException(fieldName + " must be at least " + min);
            }
            if (max != null && value > max.doubleValue()) {
                throw new ValidationException(fieldName + " cannot exceed " + max);
            }
        }
    }

    // ===== NEW VALIDATION METHODS FOR TEST SYSTEM =====

    /**
     * Validate ID fields (positive integers)
     */
    public static void validateId(Long id, String fieldName) {
        validateNotNull(id, fieldName);
        if (id <= 0) {
            throw new ValidationException(fieldName + " must be a positive number");
        }
        if (!ID_PATTERN.matcher(id.toString()).matches()) {
            throw new ValidationException(fieldName + " has invalid format");
        }
    }

    /**
     * Validate string with length constraints
     */
    public static void validateString(String value, String fieldName, int minLength, int maxLength) {
        validateNotBlank(value, fieldName);
        validateLength(value, minLength, maxLength, fieldName);
    }

    /**
     * Validate numeric range with specific bounds
     */
    public static void validateRange(Number value, String fieldName, Number min, Number max) {
        validateNotNull(value, fieldName);
        validateNumberRange(value, min, max, fieldName);
    }

    /**
     * Validate question type
     */
    public static void validateQuestionType(String questionType) {
        if (questionType == null) {
            return; // Optional field
        }
        if (!QUESTION_TYPE_PATTERN.matcher(questionType).matches()) {
            throw new ValidationException("Invalid question type format");
        }
        if (!VALID_QUESTION_TYPES.contains(questionType.toUpperCase())) {
            throw new ValidationException("Unsupported question type: " + questionType);
        }
    }

    /**
     * Validate submission reason
     */
    public static void validateSubmissionReason(String reason) {
        if (reason == null) {
            return; // Optional field
        }
        if (!VALID_SUBMISSION_REASONS.contains(reason.toUpperCase())) {
            throw new ValidationException("Invalid submission reason: " + reason);
        }
    }

    /**
     * Validate test status
     */
    public static void validateTestStatus(String status) {
        if (status == null) {
            throw new ValidationException("Test status cannot be null");
        }
        if (!VALID_TEST_STATUSES.contains(status.toUpperCase())) {
            throw new ValidationException("Invalid test status: " + status);
        }
    }

    /**
     * Validate IP address format
     */
    public static void validateIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return; // Optional field
        }
        if (!IP_ADDRESS_PATTERN.matcher(ipAddress).matches()) {
            throw new ValidationException("Invalid IP address format: " + ipAddress);
        }
    }

    /**
     * Validate user agent string
     */
    public static void validateUserAgent(String userAgent) {
        if (userAgent == null) {
            return; // Optional field
        }
        if (!USER_AGENT_PATTERN.matcher(userAgent).matches()) {
            throw new ValidationException("Invalid user agent format");
        }
    }

    /**
     * Validate test answer content based on question type
     */
    public static void validateAnswerContent(String answer, String questionType) {
        validateNotBlank(answer, "Answer");

        if (questionType != null) {
            switch (questionType.toUpperCase()) {
                case "MULTIPLE_CHOICE":
                    validateMultipleChoiceAnswer(answer);
                    break;
                case "TRUE_FALSE":
                    validateTrueFalseAnswer(answer);
                    break;
                case "SHORT_ANSWER":
                    validateShortAnswer(answer);
                    break;
                case "ESSAY":
                    validateEssayAnswer(answer);
                    break;
                case "NUMERIC":
                    validateNumericAnswer(answer);
                    break;
                default:
                    validateGenericAnswer(answer);
            }
        } else {
            validateGenericAnswer(answer);
        }
    }

    private static void validateMultipleChoiceAnswer(String answer) {
        if (answer.length() > 10) {
            throw new ValidationException("Multiple choice answers cannot exceed 10 characters");
        }
        if (!answer.matches("^[A-Za-z0-9]{1,10}$")) {
            throw new ValidationException("Multiple choice answers must be alphanumeric");
        }
    }

    private static void validateTrueFalseAnswer(String answer) {
        if (!answer.equalsIgnoreCase("true") && !answer.equalsIgnoreCase("false") &&
                !answer.equalsIgnoreCase("t") && !answer.equalsIgnoreCase("f")) {
            throw new ValidationException("True/false answers must be 'true', 'false', 't', or 'f'");
        }
    }

    private static void validateShortAnswer(String answer) {
        if (answer.length() > 500) {
            throw new ValidationException("Short answers cannot exceed 500 characters");
        }
    }

    private static void validateEssayAnswer(String answer) {
        if (answer.length() < 50) {
            throw new ValidationException("Essay answers must be at least 50 characters");
        }
        if (answer.length() > 10000) {
            throw new ValidationException("Essay answers cannot exceed 10,000 characters");
        }
    }

    private static void validateNumericAnswer(String answer) {
        try {
            Double.parseDouble(answer);
        } catch (NumberFormatException e) {
            throw new ValidationException("Numeric answers must be valid numbers");
        }
    }

    private static void validateGenericAnswer(String answer) {
        if (answer.trim().isEmpty()) {
            throw new ValidationException("Answer cannot be empty or whitespace only");
        }
        if (SanitizationUtils.containsSuspiciousPatterns(answer)) {
            throw new ValidationException("Answer contains suspicious content");
        }
    }

    // ===== EXISTING SANITIZATION METHODS =====

    public static String sanitizeInput(String input) {
        if (input == null) return null;

        return input.trim()
                .replaceAll("<script>", "")
                .replaceAll("</script>", "")
                .replaceAll("javascript:", "")
                .replaceAll("onload=", "")
                .replaceAll("onerror=", "")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;");
    }

    public static String sanitizeName(String name) {
        if (name == null) return null;
        return name.trim().replaceAll("[^a-zA-Z\\s'-]", "");
    }

    public static String sanitizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }

    // ===== EXISTING COMPOSITE VALIDATION METHODS =====

    public static void validateUserRegistration(String email, String password, String firstName, String lastName) {
        validateEmail(email);
        validatePassword(password);
        validateName(firstName, "First name");
        validateName(lastName, "Last name");
        validateLength(password, 8, 128, "Password");
    }

    public static void validateTestCreation(String title, String description, Double duration, Double fee) {
        validateNotBlank(title, "Test title");
        validateMinLength(title, 3, "Test title");
        validateMaxLength(title, 255, "Test title");
        validateMaxLength(description, 1000, "Test description");
        validatePositiveNumber(duration, "Duration");
        validateNonNegativeNumber(fee, "Fee");
    }

    // ===== NEW COMPOSITE VALIDATION METHODS =====

    /**
     * Validate test attempt submission request
     */
    public static void validateSubmitAnswerRequest(Long attemptId, Long userId, Long questionId,
                                                   String answer, Integer timeSpentSeconds, String questionType) {
        validateId(attemptId, "attemptId");
        validateId(userId, "userId");
        validateId(questionId, "questionId");
        validateRange(timeSpentSeconds, "timeSpentSeconds", 0, 3600);

        if (questionType != null) {
            validateQuestionType(questionType);
        }

        validateAnswerContent(answer, questionType);
    }

    /**
     * Validate test submission request
     */
    public static void validateSubmitTestRequest(Long attemptId, Long userId, String submissionReason) {
        validateId(attemptId, "attemptId");
        validateId(userId, "userId");

        if (submissionReason != null) {
            validateSubmissionReason(submissionReason);
        }
    }

    // ===== EXISTING UTILITY METHODS =====

    public static boolean isNumeric(String str) {
        return isNotBlank(str) && str.matches("-?\\d+(\\.\\d+)?");
    }

    public static boolean isInteger(String str) {
        return isNotBlank(str) && str.matches("-?\\d+");
    }

    public static String getValidationErrorMessage(Exception e) {
        if (e instanceof ValidationException || e instanceof IllegalArgumentException) {
            return e.getMessage();
        }
        return "Validation error occurred";
    }
}