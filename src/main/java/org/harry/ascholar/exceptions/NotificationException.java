package org.harry.ascholar.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NotificationException extends BusinessException {
    private final String notificationType;
    private final Long userId;
    private final Map<String, Object> context;

    public NotificationException(String message) {
        super(message, "NOTIFICATION_ERROR", HttpStatus.BAD_REQUEST);
        this.notificationType = null;
        this.userId = null;
        this.context = null;
    }

    public NotificationException(String message, String notificationType) {
        super(message, "NOTIFICATION_ERROR", HttpStatus.BAD_REQUEST);
        this.notificationType = notificationType;
        this.userId = null;
        this.context = null;
    }

    public NotificationException(String message, String notificationType, Long userId) {
        super(String.format("Notification error for user %d: %s", userId, message),
                "NOTIFICATION_ERROR", HttpStatus.BAD_REQUEST);
        this.notificationType = notificationType;
        this.userId = userId;
        this.context = null;
    }

    public NotificationException(String message, String notificationType, Long userId, Throwable cause) {
        super(String.format("Notification error for user %d: %s", userId, message),
                "NOTIFICATION_ERROR", HttpStatus.BAD_REQUEST, cause);
        this.notificationType = notificationType;
        this.userId = userId;
        this.context = null;
    }

    public NotificationException(String message, String notificationType, Long userId,
                                 Map<String, Object> context, Throwable cause) {
        super(String.format("Notification error for user %d: %s", userId, message),
                "NOTIFICATION_ERROR", HttpStatus.BAD_REQUEST, cause);
        this.notificationType = notificationType;
        this.userId = userId;
        this.context = context;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public Long getUserId() {
        return userId;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public String getErrorCode() {
        return "NOTIFICATION_ERROR";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    public boolean hasUserId() {
        return userId != null;
    }

    public boolean hasNotificationType() {
        return notificationType != null && !notificationType.trim().isEmpty();
    }

    public boolean hasContext() {
        return context != null && !context.isEmpty();
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder(getMessage());

        if (hasNotificationType()) {
            sb.append(" [Type: ").append(notificationType).append("]");
        }

        if (hasUserId()) {
            sb.append(" [User: ").append(userId).append("]");
        }

        if (hasContext()) {
            sb.append(" [Context: ").append(context).append("]");
        }

        return sb.toString();
    }
}