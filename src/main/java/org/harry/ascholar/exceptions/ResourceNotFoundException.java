// src/main/java/org/harry/ascholar/exceptions/ResourceNotFoundException.java
package org.harry.ascholar.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;
    private final String errorCode;
    private final HttpStatus httpStatus;

    // Existing constructors
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.errorCode = "RESOURCE_NOT_FOUND";
        this.httpStatus = HttpStatus.NOT_FOUND;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
        this.errorCode = "RESOURCE_NOT_FOUND";
        this.httpStatus = HttpStatus.NOT_FOUND;
    }

    // NEW: Add constructor with cause
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue, Throwable cause) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue), cause);
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.errorCode = "RESOURCE_NOT_FOUND";
        this.httpStatus = HttpStatus.NOT_FOUND;
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
        this.errorCode = "RESOURCE_NOT_FOUND";
        this.httpStatus = HttpStatus.NOT_FOUND;
    }

    public String getDetailedMessage() {
        if (resourceName != null && fieldName != null && fieldValue != null) {
            return String.format("%s not found with %s: '%s'. Please check if the resource exists.",
                    resourceName, fieldName, fieldValue);
        }
        return getMessage();
    }

    // Getters
    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}