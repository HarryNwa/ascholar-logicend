package org.harry.ascholar.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tests", indexes = {
        @Index(name = "idx_test_active", columnList = "is_active"),
        @Index(name = "idx_test_created", columnList = "created_at"),
        @Index(name = "idx_test_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Test {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Test title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    @Column(nullable = false, length = 255)
    private String title;

    @NotBlank(message = "Test description is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @NotNull(message = "Duration is required")
    @Column(nullable = false)
    private Integer durationMinutes; // Duration in minutes

    @NotNull(message = "Fee is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Fee must be greater than 0")
    @Digits(integer = 6, fraction = 2, message = "Fee must have up to 6 integer and 2 fraction digits")
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fee;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String category; // e.g., "MATH", "SCIENCE", "ENGLISH"

    @Column(name = "total_questions")
    @Min(value = 1, message = "Total questions must be at least 1")
    private Integer totalQuestions;

    @Column(name = "passing_score", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Passing score cannot be negative")
    @DecimalMax(value = "100.0", message = "Passing score cannot exceed 100")
    private BigDecimal passingScore;

    @Column(name = "max_attempts")
    @Min(value = 1, message = "Max attempts must be at least 1")
    private Integer maxAttempts = 1;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    @Column(name = "instructions", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Instructions cannot exceed 2000 characters")
    private String instructions;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Column(name = "requires_payment", nullable = false)
    private Boolean requiresPayment = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // Relationships
//    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<TestAttempt> testAttempts = new ArrayList<>();

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestAttempt> testAttempts = new ArrayList<>();


    // Business methods
    public Duration getDuration() {
        return Duration.ofMinutes(durationMinutes);
    }

    public void setDuration(Duration duration) {
        this.durationMinutes = (int) duration.toMinutes();
    }

    public boolean isRegistrationOpen() {
        if (registrationDeadline == null) {
            return true; // No deadline means always open
        }
        return LocalDateTime.now().isBefore(registrationDeadline);
    }

    public boolean isCurrentlyAvailable() {
        LocalDateTime now = LocalDateTime.now();
        boolean withinTimeRange = true;

        if (startTime != null) {
            withinTimeRange = now.isAfter(startTime);
        }
        if (endTime != null) {
            withinTimeRange = withinTimeRange && now.isBefore(endTime);
        }

        return isActive && withinTimeRange;
    }

    public boolean canUserAttempt(Long userId) {
        if (!isActive || !isCurrentlyAvailable()) {
            return false;
        }

        // Check if user hasn't exceeded max attempts
        long userAttemptCount = testAttempts.stream()
                .filter(attempt -> attempt.getUser() != null && userId.equals(attempt.getUser().getId()))
                .count();

        return userAttemptCount < maxAttempts;
    }

    public BigDecimal calculateFeeWithTax(BigDecimal taxRate) {
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) == 0) {
            return fee;
        }
        return fee.multiply(BigDecimal.ONE.add(taxRate));
    }

    @PrePersist
    @PreUpdate
    private void validateTimings() {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalStateException("Test start time cannot be after end time");
        }

        if (registrationDeadline != null && startTime != null &&
                registrationDeadline.isAfter(startTime)) {
            throw new IllegalStateException("Registration deadline cannot be after test start time");
        }
    }
}