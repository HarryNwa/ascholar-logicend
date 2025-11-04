// src/main/java/org/harry/ascholar/service/SecurityService.java
package org.harry.ascholar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.harry.ascholar.data.models.TestAttempt;
import org.harry.ascholar.data.models.User;
import org.harry.ascholar.data.repo.TestAttemptRepository;
import org.harry.ascholar.exceptions.SecurityException;
import org.harry.ascholar.exceptions.TestAttemptNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {

    private final TestAttemptRepository testAttemptRepository;
    private final UserService userService;

    /**
     * Check if user can access a specific test attempt
     * Used in @PreAuthorize annotations
     */
    public boolean canAccessTestAttempt(Long attemptId, Long userId) {
        log.debug("Checking access for attempt: {} and user: {}", attemptId, userId);

        // Validate inputs
        if (attemptId == null || userId == null) {
            throw new SecurityException("Attempt ID and User ID cannot be null");
        }

        try {
            // Get current authenticated user
            User currentUser = getCurrentAuthenticatedUser();

            // Admin users can access any attempt
            if (currentUser.isAdmin() || currentUser.isUniversityAdmin() || currentUser.isAnalyst()) {
                log.debug("Admin/analyst access granted for attempt: {}", attemptId);
                return true;
            }

            // Regular users can only access their own attempts
            if (!Objects.equals(currentUser.getId(), userId)) {
                log.warn("User {} attempted to access attempt {} belonging to user {}",
                        currentUser.getId(), attemptId, userId);
                return false;
            }

            // Verify the attempt actually belongs to the user
            TestAttempt attempt = testAttemptRepository.findById(attemptId)
                    .orElseThrow(() -> new TestAttemptNotFoundException(attemptId));

            boolean hasAccess = Objects.equals(attempt.getUser().getId(), userId);

            if (!hasAccess) {
                log.warn("Access denied - Attempt {} does not belong to user {}", attemptId, userId);
            }

            return hasAccess;

        } catch (Exception e) {
            log.error("Security check failed for attempt: {}, user: {}", attemptId, userId, e);
            throw new SecurityException("Security validation failed: " + e.getMessage());
        }
    }

    /**
     * Check if user can modify test content
     */
    public boolean canModifyTest(Long testId, Long userId) {
        log.debug("Checking test modification permissions for test: {} and user: {}", testId, userId);

        User currentUser = getCurrentAuthenticatedUser();

        // Admin, university admin, and content managers can modify tests
        boolean canModify = currentUser.isAdmin() || currentUser.isUniversityAdmin() || currentUser.isContentManager();

        if (!canModify) {
            log.warn("User {} attempted to modify test {} without sufficient permissions",
                    currentUser.getId(), testId);
        }

        return canModify;
    }

    /**
     * Check if user can view student results
     */
    public boolean canViewStudentResults(Long studentId, Long userId) {
        log.debug("Checking result view permissions for student: {} and user: {}", studentId, userId);

        User currentUser = getCurrentAuthenticatedUser();

        // Admin, university admin, and analysts can view any student results
        if (currentUser.isAdmin() || currentUser.isUniversityAdmin() || currentUser.isAnalyst()) {
            return true;
        }

        // Students can only view their own results
        if (currentUser.isStudent()) {
            boolean canView = Objects.equals(currentUser.getId(), studentId);
            if (!canView) {
                log.warn("Student {} attempted to view results of student {}",
                        currentUser.getId(), studentId);
            }
            return canView;
        }

        return false;
    }

    /**
     * Check if user can manage payments
     */
    public boolean canManagePayments(Long userId) {
        User currentUser = getCurrentAuthenticatedUser();

        // Only admin and finance managers can manage payments
        boolean canManage = currentUser.isAdmin() || currentUser.isFinanceManager();

        if (!canManage) {
            log.warn("User {} attempted to manage payments without sufficient permissions",
                    currentUser.getId());
        }

        return canManage;
    }

    /**
     * Check if user can process refunds
     */
    public boolean canProcessRefunds(Long userId) {
        User currentUser = getCurrentAuthenticatedUser();

        // Only finance managers and admins can process refunds
        boolean canProcess = currentUser.isAdmin() || currentUser.isFinanceManager();

        if (!canProcess) {
            log.warn("User {} attempted to process refunds without sufficient permissions",
                    currentUser.getId());
        }

        return canProcess;
    }

    /**
     * Check if user can view financial reports
     */
    public boolean canViewFinancialReports(Long userId) {
        User currentUser = getCurrentAuthenticatedUser();

        // Admins, finance managers, and analysts can view financial reports
        boolean canView = currentUser.isAdmin() || currentUser.isFinanceManager() || currentUser.isAnalyst();

        if (!canView) {
            log.warn("User {} attempted to view financial reports without sufficient permissions",
                    currentUser.getId());
        }

        return canView;
    }

    /**
     * Check if user can manage content (tests, questions, etc.)
     */
    public boolean canManageContent(Long userId) {
        User currentUser = getCurrentAuthenticatedUser();

        // Admins, university admins, and content managers can manage content
        boolean canManage = currentUser.isAdmin() || currentUser.isUniversityAdmin() || currentUser.isContentManager();

        if (!canManage) {
            log.warn("User {} attempted to manage content without sufficient permissions",
                    currentUser.getId());
        }

        return canManage;
    }

    /**
     * Check if user can view analytics dashboard
     */
    public boolean canViewAnalytics(Long userId) {
        User currentUser = getCurrentAuthenticatedUser();

        // Only admin and analytics roles can view analytics
        boolean canView = currentUser.isAdmin() || currentUser.isAnalyst();

        if (!canView) {
            log.warn("User {} attempted to view analytics without sufficient permissions",
                    currentUser.getId());
        }

        return canView;
    }

    /**
     * Check if user can export data
     */
    public boolean canExportData(Long userId) {
        User currentUser = getCurrentAuthenticatedUser();

        // Admins and analysts can export data
        boolean canExport = currentUser.isAdmin() || currentUser.isAnalyst();

        if (!canExport) {
            log.warn("User {} attempted to export data without sufficient permissions",
                    currentUser.getId());
        }

        return canExport;
    }

    /**
     * Get current authenticated user from security context
     */
    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        String username = authentication.getName();
        log.debug("Getting current authenticated user: {}", username);

        return userService.findByEmail(username)
                .orElseThrow(() -> new SecurityException("Authenticated user not found: " + username));
    }

    /**
     * Get current user ID from security context
     */
    public Long getCurrentUserId() {
        return getCurrentAuthenticatedUser().getId();
    }

    /**
     * Check if current user has specific role
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(role));
    }

    /**
     * Validate that user owns the test attempt
     */
    @Transactional(readOnly = true)
    public void validateAttemptOwnership(Long attemptId, Long userId) {
        if (!canAccessTestAttempt(attemptId, userId)) {
            throw new SecurityException(
                    String.format("User %d does not have access to test attempt %d", userId, attemptId)
            );
        }
    }

    /**
     * Check if user can access test for registration
     */
    public boolean canRegisterForTest(Long testId, Long userId) {
        log.debug("Checking test registration permissions for test: {} and user: {}", testId, userId);

        User currentUser = getCurrentAuthenticatedUser();

        // Students can register for tests, admins can register on behalf of students
        boolean canRegister = currentUser.isStudent() || currentUser.isAdmin() || currentUser.isUniversityAdmin();

        if (!canRegister) {
            log.warn("User {} attempted to register for test {} without sufficient permissions",
                    currentUser.getId(), testId);
        }

        return canRegister;
    }

    /**
     * Check if user can manage university content
     */
    public boolean canManageUniversityContent(Long universityId, Long userId) {
        User currentUser = getCurrentAuthenticatedUser();

        // Platform admins can manage all university content
        if (currentUser.isAdmin()) {
            return true;
        }

        // University admins can only manage their own university content
        if (currentUser.isUniversityAdmin()) {
            boolean canManage = currentUser.getUniversity() != null &&
                    currentUser.getUniversity().getId().equals(universityId);
            if (!canManage) {
                log.warn("University admin {} attempted to manage content for university {}",
                        currentUser.getId(), universityId);
            }
            return canManage;
        }

        // Content managers can manage content for any university
        if (currentUser.isContentManager()) {
            return true;
        }

        return false;
    }

    /**
     * Check if user can create/manage tests
     */
    public boolean canCreateTests(Long userId) {
        User currentUser = getCurrentAuthenticatedUser();
        return currentUser.isAdmin() || currentUser.isUniversityAdmin() || currentUser.isContentManager();
    }

    /**
     * Check if user can grade tests
     */
    public boolean canGradeTests(Long userId) {
        User currentUser = getCurrentAuthenticatedUser();
        return currentUser.isAdmin() || currentUser.isUniversityAdmin() || currentUser.isTutor() || currentUser.isContentManager();
    }

    /**
     * Check if user can manage user accounts
     */
    public boolean canManageUsers(Long userId) {
        User currentUser = getCurrentAuthenticatedUser();
        return currentUser.isAdmin() || currentUser.isUniversityAdmin();
    }

    /**
     * Check if user can access system settings
     */
    public boolean canAccessSystemSettings(Long userId) {
        User currentUser = getCurrentAuthenticatedUser();
        return currentUser.isAdmin();
    }
}