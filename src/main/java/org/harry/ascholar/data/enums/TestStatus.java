package org.harry.ascholar.data.enums;

public enum TestStatus {
    REGISTERED,      // Signed up, payment pending
    PAYMENT_PENDING, // Payment processing
    PAYMENT_VERIFIED,// Payment confirmed, ready to start
    IN_PROGRESS,     // Currently taking test
    PAUSED,          // Temporarily stopped
    COMPLETED,       // Finished, not graded
    AUTO_SUBMITTED,  // Time expired
    GRADED,          // Scored and evaluated
    UNDER_REVIEW,    // Flagged for cheating
    DISQUALIFIED,    // Cheating confirmed
    CANCELLED        // User cancelled
}