package org.harry.ascholar.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentNotVerifiedException extends BusinessException {
    private final Long attemptId;
    private final String paymentIntentId;

    public PaymentNotVerifiedException(Long attemptId) {
        super(String.format("Payment not verified for test attempt %d", attemptId));
        this.attemptId = attemptId;
        this.paymentIntentId = null;
    }

    public PaymentNotVerifiedException(Long attemptId, String paymentIntentId) {
        super(String.format("Payment not verified for test attempt %d (Payment Intent: %s)",
                attemptId, paymentIntentId));
        this.attemptId = attemptId;
        this.paymentIntentId = paymentIntentId;
    }

    public PaymentNotVerifiedException(Long attemptId, String paymentIntentId, Throwable cause) {
        super(String.format("Payment not verified for test attempt %d (Payment Intent: %s)",
                attemptId, paymentIntentId), cause);
        this.attemptId = attemptId;
        this.paymentIntentId = paymentIntentId;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    @Override
    public String getErrorCode() {
        return "PAYMENT_NOT_VERIFIED";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.PAYMENT_REQUIRED;
    }

    public boolean hasPaymentIntent() {
        return paymentIntentId != null && !paymentIntentId.trim().isEmpty();
    }
}