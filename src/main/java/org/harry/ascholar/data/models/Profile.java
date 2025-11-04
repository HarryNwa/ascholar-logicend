package org.harry.ascholar.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String location;

    @Column(name = "desired_program")
    private String desiredProgram;

    @Column(name = "degree_level")
    private String degreeLevel;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore = BigDecimal.ZERO;

    @Column(name = "last_test_taken")
    private LocalDateTime lastTestTaken;

    @Column(name = "profile_completion_percentage")
    private Integer profileCompletionPercentage = 0;

    @Column(name = "is_visible_to_universities")
    private Boolean isVisibleToUniversities = true;

    @Column(name = "resume_url")
    private String resumeUrl;

    @Column(name = "transcript_url")
    private String transcriptUrl;

    @ElementCollection
    @CollectionTable(name = "profile_test_scores", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "test_id")
    @Column(name = "score")
    private Map<Long, BigDecimal> testScores = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "profile_education", joinColumns = @JoinColumn(name = "profile_id"))
    private List<Education> educationHistory = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void calculateProfileCompletion() {
        int completion = 0;
        int totalFields = 8; // Adjust based on required fields

        if (firstName != null && !firstName.trim().isEmpty()) completion++;
        if (lastName != null && !lastName.trim().isEmpty()) completion++;
        if (location != null && !location.trim().isEmpty()) completion++;
        if (desiredProgram != null && !desiredProgram.trim().isEmpty()) completion++;
        if (degreeLevel != null && !degreeLevel.trim().isEmpty()) completion++;
        if (skills != null && !skills.isEmpty()) completion++;
        if (resumeUrl != null && !resumeUrl.trim().isEmpty()) completion++;
        if (isVisibleToUniversities != null) completion++;

        this.profileCompletionPercentage = (completion * 100) / totalFields;
    }

    public void addTestScore(Long testId, BigDecimal score) {
        if (this.testScores == null) {
            this.testScores = new HashMap<>();
        }
        this.testScores.put(testId, score);
        calculateOverallScore();
    }

    private void calculateOverallScore() {
        if (testScores == null || testScores.isEmpty()) {
            this.overallScore = BigDecimal.ZERO;
            return;
        }

        BigDecimal sum = testScores.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.overallScore = sum.divide(BigDecimal.valueOf(testScores.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    @PrePersist
    @PreUpdate
    private void onSave() {
        calculateProfileCompletion();
        if (this.testScores == null) {
            this.testScores = new HashMap<>();
        }
    }
}