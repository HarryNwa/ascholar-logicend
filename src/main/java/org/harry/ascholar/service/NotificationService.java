package org.harry.ascholar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.harry.ascholar.data.models.Notification;
import org.harry.ascholar.data.models.User;
import org.harry.ascholar.data.repo.NotificationRepository;
import org.harry.ascholar.exceptions.NotificationException;
import org.harry.ascholar.exceptions.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final RateLimitService rateLimitService;

    private final Map<String, LocalDateTime> notificationRateLimit = new ConcurrentHashMap<>();
    private static final String NOTIFICATION_CACHE = "notifications";

    // Basic notification creation with title
    @Transactional
    public Notification createNotification(Long userId, String type, String title, String message, String actionUrl) {
        return createNotification(userId, type, title, message, actionUrl, null, null, null);
    }

    // Notification with metadata
    @Transactional
    @CacheEvict(value = NOTIFICATION_CACHE, key = "#userId")
    public Notification createNotification(Long userId, String type, String title, String message, String actionUrl,
                                           Map<String, Object> metadata) {
        return createNotification(userId, type, title, message, actionUrl, metadata, null, null);
    }

    // Full notification creation with all parameters
    @Transactional
    @CacheEvict(value = NOTIFICATION_CACHE, key = "#userId")
    public Notification createNotification(Long userId, String type, String title, String message, String actionUrl,
                                           Map<String, Object> metadata, String category, Notification.NotificationPriority priority) {
        try {
            // Rate limiting per user
            String rateLimitKey = "notification:" + userId;
            if (!rateLimitService.tryAcquire(rateLimitKey, java.time.Duration.ofSeconds(10))) {
                log.warn("Notification rate limit exceeded for user: {}", userId);
                throw new NotificationException("Notification rate limit exceeded", type, userId);
            }

            User user = userService.getUserById(userId);

            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType(validateNotificationType(type));
            notification.setTitle(validateAndSanitizeTitle(title));
            notification.setMessage(validateAndSanitizeMessage(message));
            notification.setActionUrl(validateActionUrl(actionUrl));
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setMetadata(metadata != null ? new HashMap<>(metadata) : null); // Defensive copy
            notification.setCategory(category != null ? category : determineCategoryFromType(type));
            notification.setPriority(priority != null ? priority : determinePriorityFromType(type));

            Notification savedNotification = notificationRepository.save(notification);

            log.info("Created notification for user {}: {} - {}", userId, type, title);
            return savedNotification;

        } catch (Exception e) {
            log.error("Failed to create notification for user {}: {}", userId, e.getMessage());
            throw new NotificationException("Failed to create notification: " + e.getMessage(), type, userId, e);
        }
    }

    // Simple notification without title (auto-generates title from type)
    @Transactional
    public Notification createSimpleNotification(Long userId, String type, String message, String actionUrl) {
        String title = generateTitleFromType(type);
        return createNotification(userId, type, title, message, actionUrl, null, null, null);
    }

    // Factory methods for common notification types
    @Transactional
    public Notification createTestRegistrationNotification(Long userId, Long testId, String testTitle) {
        Map<String, Object> metadata = Map.of(
                "testId", testId,
                "testTitle", testTitle,
                "timestamp", LocalDateTime.now().toString()
        );

        return Notification.createTestRegistrationNotification(
                userService.getUserById(userId), testId, testTitle
        );
    }

    @Transactional
    public Notification createTestCompletionNotification(Long userId, Long attemptId, String testTitle, String score) {
        Map<String, Object> metadata = Map.of(
                "attemptId", attemptId,
                "testTitle", testTitle,
                "score", score,
                "completedAt", LocalDateTime.now().toString()
        );

        return createNotification(
                userId,
                "TEST_COMPLETED",
                "Test Completed",
                String.format("You have completed '%s' with score: %s", testTitle, score),
                String.format("/student/results/%d", attemptId),
                metadata,
                "TEST",
                Notification.NotificationPriority.MEDIUM
        );
    }

    @Transactional
    public Notification createUniversityInterestNotification(Long userId, String universityName, Long universityId) {
        Map<String, Object> metadata = Map.of(
                "universityId", universityId,
                "universityName", universityName,
                "expressedAt", LocalDateTime.now().toString()
        );

        return createNotification(
                userId,
                "UNIVERSITY_INTEREST",
                "University Interest",
                String.format("%s has expressed interest in your profile", universityName),
                "/student/opportunities",
                metadata,
                "UNIVERSITY",
                Notification.NotificationPriority.HIGH
        );
    }

    @Transactional
    public Notification createPaymentConfirmedNotification(Long userId, Long paymentId, String amount, String testTitle) {
        Map<String, Object> metadata = Map.of(
                "paymentId", paymentId,
                "amount", amount,
                "testTitle", testTitle,
                "confirmedAt", LocalDateTime.now().toString()
        );

        return createNotification(
                userId,
                "PAYMENT_CONFIRMED",
                "Payment Confirmed",
                String.format("Your payment of %s for '%s' has been confirmed", amount, testTitle),
                String.format("/payments/%d", paymentId),
                metadata,
                "PAYMENT",
                Notification.NotificationPriority.MEDIUM
        );
    }

    @Transactional
    public Notification createSystemAnnouncement(Long userId, String title, String message, String actionUrl,
                                                 LocalDateTime expiresAt) {
        Map<String, Object> metadata = Map.of(
                "announcementType", "SYSTEM",
                "issuedAt", LocalDateTime.now().toString()
        );

        Notification notification = createNotification(
                userId,
                "SYSTEM_ANNOUNCEMENT",
                title,
                message,
                actionUrl,
                metadata,
                "SYSTEM",
                Notification.NotificationPriority.LOW
        );

        notification.setExpiresAt(expiresAt);
        return notificationRepository.save(notification);
    }

    // Existing methods remain the same...
    @Cacheable(value = NOTIFICATION_CACHE, key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        log.debug("Fetching notifications for user: {}, page: {}", userId, pageable.getPageNumber());
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Cacheable(value = NOTIFICATION_CACHE, key = "#userId + '_unread'")
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    @CacheEvict(value = NOTIFICATION_CACHE, key = "#userId")
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);
            log.debug("Marked notification {} as read for user {}", notificationId, userId);
        }
    }

    @Transactional
    @CacheEvict(value = NOTIFICATION_CACHE, key = "#userId")
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        LocalDateTime now = LocalDateTime.now();

        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
        }

        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked all {} notifications as read for user {}", unreadNotifications.size(), userId);
    }

    @Transactional
    @CacheEvict(value = NOTIFICATION_CACHE, key = "#userId")
    public void deleteNotification(Long notificationId, Long userId) {
        if (!notificationRepository.existsByIdAndUserId(notificationId, userId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }
        notificationRepository.deleteById(notificationId);
        log.debug("Deleted notification {} for user {}", notificationId, userId);
    }

    @Cacheable(value = NOTIFICATION_CACHE, key = "#userId + '_count'")
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    @CacheEvict(value = NOTIFICATION_CACHE, allEntries = true)
    public void bulkCreateNotifications(List<Long> userIds, String type, String title, String message, String actionUrl) {
        if (userIds.size() > 1000) {
            throw new NotificationException("Bulk notification limit exceeded. Max 1000 users per batch.", type, null);
        }

        List<Notification> notifications = userIds.stream()
                .map(userId -> {
                    Notification notification = new Notification();
                    notification.setUser(User.builder().id(userId).build()); // Proxy user
                    notification.setType(validateNotificationType(type));
                    notification.setTitle(validateAndSanitizeTitle(title));
                    notification.setMessage(validateAndSanitizeMessage(message));
                    notification.setActionUrl(validateActionUrl(actionUrl));
                    notification.setRead(false);
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setCategory(determineCategoryFromType(type));
                    notification.setPriority(determinePriorityFromType(type));
                    return notification;
                })
                .collect(java.util.stream.Collectors.toList());

        notificationRepository.saveAll(notifications);
        log.info("Created {} bulk notifications of type: {}", notifications.size(), type);
    }

    // Clean up old notifications (run daily)
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    @CacheEvict(value = NOTIFICATION_CACHE, allEntries = true)
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(6); // Keep 6 months
        long deletedCount = notificationRepository.deleteByReadTrueAndCreatedAtBefore(cutoffDate);
        log.info("Cleaned up {} old read notifications", deletedCount);

        // Also clean up expired notifications
        LocalDateTime currentDate = LocalDateTime.now();
        List<Notification> expiredNotifications = notificationRepository.findExpiredNotifications(currentDate);
        if (!expiredNotifications.isEmpty()) {
            notificationRepository.deleteAll(expiredNotifications);
            log.info("Cleaned up {} expired notifications", expiredNotifications.size());
        }
    }

    // Validation methods
    private String validateNotificationType(String type) {
        List<String> allowedTypes = List.of(
                "TEST_REGISTERED", "TEST_COMPLETED", "TEST_AUTO_SUBMITTED",
                "UNIVERSITY_INTEREST", "HIGH_PERFORMER", "PAYMENT_CONFIRMED",
                "PROFILE_UPDATED", "SYSTEM_ANNOUNCEMENT", "SECURITY_ALERT"
        );

        if (!allowedTypes.contains(type)) {
            throw new NotificationException("Invalid notification type: " + type, type, null);
        }
        return type;
    }

    private String validateAndSanitizeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new NotificationException("Notification title cannot be empty", null, null);
        }

        String sanitized = sanitizeInput(title.trim());
        if (sanitized.length() > 255) {
            throw new NotificationException("Notification title too long", null, null);
        }

        return sanitized;
    }

    private String validateAndSanitizeMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new NotificationException("Notification message cannot be empty", null, null);
        }

        String sanitized = sanitizeInput(message.trim());
        if (sanitized.length() > 1000) {
            throw new NotificationException("Notification message too long", null, null);
        }

        return sanitized;
    }

    private String validateActionUrl(String actionUrl) {
        if (actionUrl == null) {
            return null;
        }

        if (!actionUrl.startsWith("/") || actionUrl.length() > 500) {
            throw new NotificationException("Invalid action URL", null, null);
        }

        return actionUrl;
    }

    private String generateTitleFromType(String type) {
        return switch (type) {
            case "TEST_REGISTERED" -> "Test Registration Confirmed";
            case "TEST_COMPLETED" -> "Test Completed";
            case "UNIVERSITY_INTEREST" -> "University Interest";
            case "PAYMENT_CONFIRMED" -> "Payment Confirmed";
            case "SYSTEM_ANNOUNCEMENT" -> "System Announcement";
            case "PROFILE_UPDATED" -> "Profile Updated";
            case "HIGH_PERFORMER" -> "High Performer";
            case "SECURITY_ALERT" -> "Security Alert";
            default -> "Notification";
        };
    }

    private String determineCategoryFromType(String type) {
        if (type.startsWith("TEST")) return "TEST";
        if (type.startsWith("UNIVERSITY")) return "UNIVERSITY";
        if (type.startsWith("PAYMENT")) return "PAYMENT";
        if (type.startsWith("SYSTEM")) return "SYSTEM";
        if (type.startsWith("SECURITY")) return "SECURITY";
        return "GENERAL";
    }

    private Notification.NotificationPriority determinePriorityFromType(String type) {
        return switch (type) {
            case "UNIVERSITY_INTEREST", "HIGH_PERFORMER", "SECURITY_ALERT" ->
                    Notification.NotificationPriority.HIGH;
            case "TEST_COMPLETED", "PAYMENT_CONFIRMED" ->
                    Notification.NotificationPriority.MEDIUM;
            default -> Notification.NotificationPriority.LOW;
        };
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;

        return input
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replaceAll("<script>", "")
                .replaceAll("</script>", "")
                .replaceAll("javascript:", "");
    }
}