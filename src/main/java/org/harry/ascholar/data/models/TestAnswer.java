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
import java.time.LocalDateTime;

@Entity
@Table(name = "test_answers", indexes = {
        @Index(name = "idx_answer_attempt", columnList = "attempt_id"),
        @Index(name = "idx_answer_question", columnList = "question_id"),
        @Index(name = "idx_answer_attempt_question", columnList = "attempt_id,question_id", unique = true),
        @Index(name = "idx_answer_answered_at", columnList = "answered_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Test attempt is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false, foreignKey = @ForeignKey(name = "fk_answer_attempt"))
    private TestAttempt attempt;

    @NotNull(message = "Question ID is required")
    @Positive(message = "Question ID must be positive")
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @NotBlank(message = "Answer cannot be blank")
    @Size(max = 10000, message = "Answer cannot exceed 10,000 characters")
    @Column(columnDefinition = "text", nullable = false)
    private String answer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @NotNull(message = "Answered timestamp is required")
    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    @NotNull(message = "Time spent is required")
    @Min(value = 0, message = "Time spent cannot be negative")
    @Max(value = 3600, message = "Time spent cannot exceed 3600 seconds")
    @Column(name = "time_spent_seconds", nullable = false)
    private Integer timeSpentOnQuestion;

    @Column(name = "question_type", length = 50)
    @Size(max = 50, message = "Question type cannot exceed 50 characters")
    private String questionType; // MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, ESSAY, NUMERIC

    @Column(name = "question_points")
    @Min(value = 0, message = "Question points cannot be negative")
    @Max(value = 100, message = "Question points cannot exceed 100")
    private Integer questionPoints = 1;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Correct answer cannot exceed 1000 characters")
    private String correctAnswer;

    @Column(name = "points_awarded", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Points awarded cannot be negative")
    private BigDecimal pointsAwarded;

    @Column(name = "is_reviewed")
    private Boolean isReviewed = false;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Reviewer notes cannot exceed 1000 characters")
    private String reviewerNotes;

    @Column(name = "confidence_score")
    @Min(value = 0, message = "Confidence score cannot be negative")
    @Max(value = 100, message = "Confidence score cannot exceed 100")
    private Integer confidenceScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // Business methods
    public boolean isGraded() {
        return isCorrect != null;
    }

    public boolean isCorrectAnswer() {
        return Boolean.TRUE.equals(isCorrect);
    }

    public boolean isEssayType() {
        return "ESSAY".equalsIgnoreCase(questionType);
    }

    public boolean requiresManualGrading() {
        return isEssayType() && !isGraded();
    }

    public void markCorrect(BigDecimal points) {
        this.isCorrect = true;
        this.pointsAwarded = points != null ? points : BigDecimal.valueOf(questionPoints);
        this.updatedAt = LocalDateTime.now();
    }

    public void markIncorrect() {
        this.isCorrect = false;
        this.pointsAwarded = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    public void markForReview(String notes) {
        this.isReviewed = false;
        this.reviewerNotes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public void completeReview(boolean isCorrect, String notes, BigDecimal points) {
        this.isReviewed = true;
        this.isCorrect = isCorrect;
        this.reviewerNotes = notes;
        this.pointsAwarded = isCorrect ? points : BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getMaxPossiblePoints() {
        return questionPoints != null ? BigDecimal.valueOf(questionPoints) : BigDecimal.ONE;
    }

    public boolean isAnsweredWithinTime(Integer maxTimePerQuestion) {
        if (maxTimePerQuestion == null) {
            return true;
        }
        return timeSpentOnQuestion <= maxTimePerQuestion;
    }

    @PrePersist
    private void setDefaultAnsweredAt() {
        if (this.answeredAt == null) {
            this.answeredAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    private void validateAnswer() {
        if (this.answer == null || this.answer.trim().isEmpty()) {
            throw new IllegalStateException("Answer cannot be null or empty");
        }

        if (this.timeSpentOnQuestion < 0) {
            throw new IllegalStateException("Time spent cannot be negative");
        }
    }
}