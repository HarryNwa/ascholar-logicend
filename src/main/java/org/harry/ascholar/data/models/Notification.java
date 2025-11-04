package org.harry.ascholar.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "user_id"),
        @Index(name = "idx_notification_type", columnList = "type"),
        @Index(name = "idx_notification_read_status", columnList = "read_status"),
        @Index(name = "idx_notification_created_at", columnList = "created_at"),
        @Index(name = "idx_notification_user_read", columnList = "user_id, read_status"),
        @Index(name = "idx_notification_priority", columnList = "priority")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_user"))
    private User user;

    @NotBlank(message = "Notification type is required")
    @Size(max = 50, message = "Type cannot exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String type; // TEST_REGISTERED, TEST_COMPLETED, UNIVERSITY_INTEREST, PAYMENT_CONFIRMED, etc.

    @NotBlank(message = "Notification title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String title;

    @NotBlank(message = "Notification message is required")
    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    @Column(nullable = false, length = 1000)
    private String message;

    @Size(max = 500, message = "Action URL cannot exceed 500 characters")
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "read_status", nullable = false)
    private Boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority = NotificationPriority.MEDIUM;

    @Column(name = "category", length = 50)
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category; // SYSTEM, TEST, PAYMENT, UNIVERSITY, SECURITY

    @Column(name = "source_id")
    private Long sourceId; // ID of the source entity (test_id, attempt_id, etc.)

    @Column(name = "source_type", length = 50)
    @Size(max = 50, message = "Source type cannot exceed 50 characters")
    private String sourceType; // TEST, TEST_ATTEMPT, PAYMENT, etc.

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Version
    private Long version;

    // Business methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRead() {
        return Boolean.TRUE.equals(read);
    }

    public boolean isUnread() {
        return !isRead();
    }

    public void markAsRead() {
        if (!this.read) {
            this.read = true;
            this.readAt = LocalDateTime.now();
        }
    }

    public void markAsUnread() {
        this.read = false;
        this.readAt = null;
    }

    public boolean isActionable() {
        return actionUrl != null && !actionUrl.trim().isEmpty();
    }

    public boolean belongsToUser(Long userId) {
        return user != null && user.getId().equals(userId);
    }

    public String getSummary() {
        return String.format("[%s] %s", type, title);
    }

    public boolean shouldExpire() {
        return expiresAt != null;
    }

    @PrePersist
    @PreUpdate
    private void validateNotification() {
        if (expiresAt != null && expiresAt.isBefore(createdAt)) {
            throw new IllegalStateException("Expiration date cannot be before creation date");
        }

        if (read && readAt == null) {
            readAt = LocalDateTime.now();
        }

        if (!read && readAt != null) {
            readAt = null;
        }
    }

    public enum NotificationPriority {
        LOW, MEDIUM, HIGH, URGENT
    }

    // Static factory methods for common notification types
    public static Notification createTestRegistrationNotification(User user, Long testId, String testTitle) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType("TEST_REGISTERED");
        notification.setTitle("Test Registration Confirmed");
        notification.setMessage(String.format("You have successfully registered for %s", testTitle));
        notification.setActionUrl(String.format("/student/tests/%d", testId));
        notification.setCategory("TEST");
        notification.setSourceId(testId);
        notification.setSourceType("TEST");
        notification.setPriority(NotificationPriority.MEDIUM);
        return notification;
    }

    public static Notification createTestCompletionNotification(User user, Long attemptId, String testTitle, BigDecimal score) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType("TEST_COMPLETED");
        notification.setTitle("Test Completed");
        notification.setMessage(String.format("You have completed %s with score: %s", testTitle, score));
        notification.setActionUrl(String.format("/student/results/%d", attemptId));
        notification.setCategory("TEST");
        notification.setSourceId(attemptId);
        notification.setSourceType("TEST_ATTEMPT");
        notification.setPriority(NotificationPriority.MEDIUM);
        return notification;
    }

    public static Notification createUniversityInterestNotification(User user, String universityName) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType("UNIVERSITY_INTEREST");
        notification.setTitle("University Interest");
        notification.setMessage(String.format("%s has expressed interest in your profile", universityName));
        notification.setActionUrl("/student/opportunities");
        notification.setCategory("UNIVERSITY");
        notification.setPriority(NotificationPriority.HIGH);
        return notification;
    }

    public static Notification createPaymentConfirmedNotification(User user, Long paymentId, BigDecimal amount) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType("PAYMENT_CONFIRMED");
        notification.setTitle("Payment Confirmed");
        notification.setMessage(String.format("Your payment of $%s has been confirmed", amount));
        notification.setActionUrl(String.format("/payments/%d", paymentId));
        notification.setCategory("PAYMENT");
        notification.setSourceId(paymentId);
        notification.setSourceType("PAYMENT");
        notification.setPriority(NotificationPriority.MEDIUM);
        return notification;
    }

    public static Notification createSystemAnnouncement(User user, String title, String message, String actionUrl) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType("SYSTEM_ANNOUNCEMENT");
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setActionUrl(actionUrl);
        notification.setCategory("SYSTEM");
        notification.setPriority(NotificationPriority.MEDIUM);
        notification.setExpiresAt(LocalDateTime.now().plusDays(30)); // System announcements expire after 30 days
        return notification;
    }
}