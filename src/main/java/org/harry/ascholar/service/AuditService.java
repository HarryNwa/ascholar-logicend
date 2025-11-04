package org.harry.ascholar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.harry.ascholar.data.models.AuditLog;
import org.harry.ascholar.data.repo.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> logTestRegistration(Long userId, Long testId, Long attemptId) {
        return saveAuditLog(
                AuditLog.builder()
                        .userId(userId)
                        .action("TEST_REGISTRATION")
                        .description(String.format("User %d registered for test %d with attempt %d", userId, testId, attemptId))
                        .ipAddress(getClientIp())
                        .userAgent(getUserAgent())
                        .timestamp(LocalDateTime.now())
                        .metadata(Map.of(
                                "testId", testId.toString(),
                                "attemptId", attemptId.toString()
                        ))
                        .build()
        );
    }

    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> logTestStart(Long userId, Long attemptId, String ipAddress, String userAgent) {
        return saveAuditLog(
                AuditLog.builder()
                        .userId(userId)
                        .action("TEST_START")
                        .description(String.format("User %d started test attempt %d", userId, attemptId))
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .timestamp(LocalDateTime.now())
                        .metadata(Map.of("attemptId", attemptId.toString()))
                        .build()
        );
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logAnswerSubmission(Long userId, Long attemptId, Long questionId, Integer timeSpent) {
        return saveAuditLog(
                AuditLog.builder()
                        .userId(userId)
                        .action("ANSWER_SUBMISSION")
                        .description(String.format("User %d submitted answer for question %d in attempt %d", userId, questionId, attemptId))
                        .timestamp(LocalDateTime.now())
                        .metadata(Map.of(
                                "attemptId", attemptId.toString(),
                                "questionId", questionId.toString(),
                                "timeSpent", timeSpent.toString()
                        ))
                        .build()
        );
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logTestCompletion(Long userId, Long attemptId, BigDecimal score) {
        return saveAuditLog(
                AuditLog.builder()
                        .userId(userId)
                        .action("TEST_COMPLETION")
                        .description(String.format("User %d completed test attempt %d with score %s", userId, attemptId, score))
                        .timestamp(LocalDateTime.now())
                        .metadata(Map.of(
                                "attemptId", attemptId.toString(),
                                "score", score != null ? score.toString() : "null"
                        ))
                        .build()
        );
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logPaymentFailure(Long userId, Long testId, String error) {
        return saveAuditLog(
                AuditLog.builder()
                        .userId(userId)
                        .action("PAYMENT_FAILURE")
                        .description(String.format("Payment failed for user %d for test %d: %s", userId, testId, error))
                        .timestamp(LocalDateTime.now())
                        .metadata(Map.of(
                                "testId", testId.toString(),
                                "error", error
                        ))
                        .build()
        );
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logAutoSubmitBatch(int count) {
        return saveAuditLog(
                AuditLog.builder()
                        .action("AUTO_SUBMIT_BATCH")
                        .description(String.format("Auto-submitted %d expired test attempts", count))
                        .timestamp(LocalDateTime.now())
                        .metadata(Map.of("count", String.valueOf(count)))
                        .build()
        );
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logAutoSubmitFailure(Long attemptId, String error) {
        return saveAuditLog(
                AuditLog.builder()
                        .action("AUTO_SUBMIT_FAILURE")
                        .description(String.format("Failed to auto-submit test attempt %d: %s", attemptId, error))
                        .timestamp(LocalDateTime.now())
                        .metadata(Map.of(
                                "attemptId", attemptId.toString(),
                                "error", error
                        ))
                        .build()
        );
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logScoringError(Long attemptId, String error) {
        return saveAuditLog(
                AuditLog.builder()
                        .action("SCORING_ERROR")
                        .description(String.format("Scoring error for attempt %d: %s", attemptId, error))
                        .timestamp(LocalDateTime.now())
                        .metadata(Map.of(
                                "attemptId", attemptId.toString(),
                                "error", error
                        ))
                        .build()
        );
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logProfileUpdateError(Long userId, String error) {
        return saveAuditLog(
                AuditLog.builder()
                        .userId(userId)
                        .action("PROFILE_UPDATE_ERROR")
                        .description(String.format("Failed to update profile for user %d: %s", userId, error))
                        .timestamp(LocalDateTime.now())
                        .metadata(Map.of("error", error))
                        .build()
        );
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logSecurityEvent(String action, String description, Long userId, Map<String, String> metadata) {
        return saveAuditLog(
                AuditLog.builder()
                        .userId(userId)
                        .action(action)
                        .description(description)
                        .timestamp(LocalDateTime.now())
                        .metadata(metadata)
                        .ipAddress(getClientIp())
                        .userAgent(getUserAgent())
                        .build()
        );
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logUserLogin(Long userId, String ipAddress, String userAgent, boolean success) {
        return saveAuditLog(
                AuditLog.builder()
                        .userId(userId)
                        .action(success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE")
                        .description(String.format("User %d %s from IP %s", userId, success ? "logged in" : "failed login", ipAddress))
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .timestamp(LocalDateTime.now())
                        .metadata(Map.of("success", String.valueOf(success)))
                        .build()
        );
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logUserLogout(Long userId) {
        return saveAuditLog(
                AuditLog.builder()
                        .userId(userId)
                        .action("LOGOUT")
                        .description(String.format("User %d logged out", userId))
                        .ipAddress(getClientIp())
                        .userAgent(getUserAgent())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    /**
     * Unified method to save audit logs with consistent error handling
     */
    private CompletableFuture<Void> saveAuditLog(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} for user: {}", auditLog.getAction(), auditLog.getUserId());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to save audit log for action: {}, user: {}",
                    auditLog.getAction(), auditLog.getUserId(), e);
            // Still return completed future to avoid breaking the async chain
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Get client IP from request context
     * You'll need to implement this based on your HTTP request context
     */
    private String getClientIp() {
        // Implementation depends on your framework
        // For Spring MVC, you might use RequestContextHolder
        try {
            // Example for Spring MVC:
            // HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            // return request.getRemoteAddr();
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Get user agent from request context
     */
    private String getUserAgent() {
        // Implementation depends on your framework
        try {
            // Example for Spring MVC:
            // HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            // return request.getHeader("User-Agent");
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}