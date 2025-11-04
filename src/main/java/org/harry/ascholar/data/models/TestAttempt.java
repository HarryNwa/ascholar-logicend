package org.harry.ascholar.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.harry.ascholar.data.enums.TestStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_attempts", indexes = {
        @Index(name = "idx_test_attempt_user", columnList = "user_id"),
        @Index(name = "idx_test_attempt_test", columnList = "test_id"),
        @Index(name = "idx_test_attempt_status", columnList = "status"),
        @Index(name = "idx_test_attempt_started", columnList = "started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ FIXED: Removed @NotBlank from relationships (only for Strings)
    @NotNull(message = "Test is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestAnswer> testAnswers = new ArrayList<>();

    // ✅ FIXED: Removed @NotBlank from relationships (only for Strings)
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @DecimalMin(value = "0.0", message = "Score must be at least 0")
    @DecimalMax(value = "100.0", message = "Score must be at most 100")
    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @NotNull(message = "Test status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestStatus status = TestStatus.REGISTERED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Min(value = 0, message = "Time spent cannot be negative")
    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    // ✅ FIXED: Typo in column name
    @Column(name = "tab_switch_count")
    private Integer tabSwitchCount = 0;

    // ✅ FIXED: Typo in column name (was "fullscreen_exit-count")
    @Column(name = "fullscreen_exit_count")
    private Integer fullscreenExitCount = 0;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "client_time_offset")
    private Integer clientTimeOffsetSeconds;

    @Min(value = 0, message = "Current question index cannot be negative")
    @Column(name = "current_question_index")
    private Integer currentQuestionIndex = 0;

    @Column(name = "answers_json", columnDefinition = "TEXT")
    private String answersJson;

    @Column(name = "payment_intent_id", length = 50)
    private String paymentIntentId;

    // ✅ ADDED: Missing field that's used in services
    @Column(name = "payment_verified", nullable = false)
    private Boolean paymentVerified = false;

    @DecimalMin(value = "0.0", message = "Payment amount cannot be negative")
    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Version
    private Long version;

    // Business methods
    public Duration getTimeSpent() {
        return timeSpentSeconds != null ? Duration.ofSeconds(timeSpentSeconds) : null;
    }

    public void setTimeSpent(Duration timeSpent) {
        this.timeSpentSeconds = timeSpent != null ? (int) timeSpent.getSeconds() : null;
    }

    public boolean isInProgress() {
        return TestStatus.IN_PROGRESS.equals(status);
    }

    public boolean isCompleted() {
        return TestStatus.COMPLETED.equals(status) || TestStatus.AUTO_SUBMITTED.equals(status);
    }

    public boolean isEligibleForGrading() {
        return isCompleted() && score == null;
    }

    public void recordTabSwitch() {
        this.tabSwitchCount = (tabSwitchCount == null) ? 1 : tabSwitchCount + 1;
    }

    public void recordFullscreenExit() {
        this.fullscreenExitCount = (fullscreenExitCount == null) ? 1 : fullscreenExitCount + 1;
    }

    public boolean hasSuspiciousActivity() {
        return (tabSwitchCount != null && tabSwitchCount > 3) ||
                (fullscreenExitCount != null && fullscreenExitCount > 0);
    }

    // ✅ ADDED: Convenience method for payment verification
    public void verifyPayment() {
        this.paymentVerified = true;
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ ADDED: Check if test can be started
    public boolean canStartTest() {
        return TestStatus.REGISTERED.equals(status) &&
                Boolean.TRUE.equals(paymentVerified) &&
                startedAt == null;
    }

    // ✅ ADDED: Calculate remaining time
    public Duration getRemainingTime() {
        if (startedAt == null || !isInProgress()) {
            return Duration.ZERO;
        }

        Duration elapsed = Duration.between(startedAt, LocalDateTime.now());
        Duration totalTime = test != null ? test.getDuration() : Duration.ofHours(1);
        Duration remaining = totalTime.minus(elapsed);

        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    // ✅ ADDED: Check if time has expired
    public boolean isTimeExpired() {
        return getRemainingTime().equals(Duration.ZERO) && isInProgress();
    }
}