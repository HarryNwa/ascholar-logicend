// src/main/java/org/harry/ascholar/exceptions/UserNotActiveException.java
package org.harry.ascholar.exceptions;

import org.springframework.security.core.AuthenticationException;

public class UserNotActiveException extends AuthenticationException {
    public UserNotActiveException(String message) {
        super(message);
    }

    public UserNotActiveException(String message, Throwable cause) {
        super(message, cause);
    }
}