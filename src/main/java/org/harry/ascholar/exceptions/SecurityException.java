// src/main/java/org/harry/ascholar/exceptions/SecurityException.java
package org.harry.ascholar.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown for security-related violations such as:
 * - Unauthorized access attempts
 * - Invalid permissions
 * - Security constraint violations
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class SecurityException extends RuntimeException {

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecurityException(String resource, String action, Long userId) {
        super(String.format("User %d is not authorized to %s %s", userId, action, resource));
    }

    public SecurityException(String resource, String action) {
        super(String.format("Not authorized to %s %s", action, resource));
    }

    public static SecurityException unauthorizedAccess(String resource, Long resourceId, Long userId) {
        return new SecurityException(
                String.format("User %d attempted to access %s %d without authorization", userId, resource, resourceId)
        );
    }

    public static SecurityException insufficientPermissions(String action, String requiredRole) {
        return new SecurityException(
                String.format("Insufficient permissions to %s. Required role: %s", action, requiredRole)
        );
    }

    public static SecurityException invalidToken() {
        return new SecurityException("Invalid or expired security token");
    }

    public static SecurityException accessDenied() {
        return new SecurityException("Access denied");
    }
}