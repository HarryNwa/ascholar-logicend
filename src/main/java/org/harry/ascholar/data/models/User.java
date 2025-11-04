package org.harry.ascholar.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.harry.ascholar.data.enums.UserRole;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(name = "password_hash", nullable = false, length = 255)
    private String password;

    @NotNull(message = "User role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "auth_provider")
    private String authProvider;

    @Column(name = "auth_provider_id")
    private String authProviderId;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;

    @Version
    private Long version;

    // ✅ Business methods aligned with YOUR UserRole enum
    public String getFullName() {
        if (profile != null) {
            return profile.getFullName();
        }
        return "User #" + id;
    }

    public boolean isStudent() {
        return UserRole.STUDENT.equals(role);
    }

    public boolean isTutor() {
        return UserRole.TUTOR.equals(role);
    }

    public boolean isUniversityAdmin() {
        return UserRole.UNIVERSITY_ADMIN.equals(role);
    }

    public boolean isPlatformAdmin() {
        return UserRole.ASCHOLAR_ADMIN.equals(role);
    }

    // ✅ NEW: Added missing role methods
    public boolean isContentManager() {
        return UserRole.CONTENT_MANAGER.equals(role);
    }

    public boolean isFinanceManager() {
        return UserRole.FINANCE_MANAGER.equals(role);
    }

    public boolean isAnalyst() {
        return UserRole.ANALYST.equals(role);
    }

    // ✅ Combined role checks for common use cases
    public boolean isUniversityUser() {
        return UserRole.UNIVERSITY_ADMIN.equals(role);
    }

    public boolean isAdmin() {
        return UserRole.ASCHOLAR_ADMIN.equals(role);
    }

    public boolean canTakeTests() {
        return isStudent() && Boolean.TRUE.equals(isEnabled) && Boolean.TRUE.equals(emailVerified);
    }

    public boolean canManageTests() {
        return isUniversityAdmin() || isPlatformAdmin() || isContentManager();
    }

    public boolean canViewTalentDashboard() {
        return isUniversityAdmin() || isPlatformAdmin() || isAnalyst();
    }

    public boolean canManageFinancials() {
        return isPlatformAdmin() || isFinanceManager();
    }

    public boolean canViewAnalytics() {
        return isPlatformAdmin() || isAnalyst() || isUniversityAdmin();
    }
}