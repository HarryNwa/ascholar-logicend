//package org.harry.ascholar.service;
//
//import com.stripe.exception.StripeException;
//import com.stripe.model.PaymentIntent;
//import com.stripe.model.Refund;
//import jakarta.annotation.PostConstruct;
//import lombok.Value;
//import org.slf4j.LoggerFactory;
//
//import java.math.BigDecimal;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.logging.Logger;
//
//public class PaymentService {
//        private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
//
//        @Value("${stripe.secret.key}")
//        private String secretKey;
//
//        @Value("${stripe.webhook.secret}")
//        private String webhookSecret;
//
//        @PostConstruct
//        public void init() {
//            Stripe.apiKey = secretKey;
//        }
//
//        public Map<String, Object> createPaymentForTest(BigDecimal amount, String description, Long userId) throws StripeException {
//            try {
//                // Convert amount to cents (Stripe uses smallest currency unit)
//                long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
//
//                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
//                        .setAmount(amountCents)
//                        .setCurrency("usd")
//                        .setDescription(description)
//                        .putMetadata("user_id", userId.toString())
//                        .putMetadata("product_type", "test_registration")
//                        .build();
//
//                PaymentIntent paymentIntent = PaymentIntent.create(params);
//
//                Map<String, Object> response = new HashMap<>();
//                response.put("clientSecret", paymentIntent.getClientSecret());
//                response.put("paymentIntentId", paymentIntent.getId());
//                response.put("amount", amount);
//                response.put("status", paymentIntent.getStatus());
//                response.put("currency", paymentIntent.getCurrency());
//
//                logger.info("Payment intent created: {} for user: {}", paymentIntent.getId(), userId);
//                return response;
//
//            } catch (StripeException e) {
//                logger.error("Stripe API error creating payment intent", e);
//                throw new RuntimeException("Payment processing failed: " + e.getMessage());
//            }
//        }
//
//        public PaymentIntent confirmPayment(String paymentIntentId) throws StripeException {
//            try {
//                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
//                return paymentIntent.confirm();
//            } catch (StripeException e) {
//                logger.error("Stripe API error confirming payment: {}", paymentIntentId, e);
//                throw new RuntimeException("Payment confirmation failed: " + e.getMessage());
//            }
//        }
//
//        public PaymentIntent getPaymentIntent(String paymentIntentId) throws StripeException {
//            try {
//                return PaymentIntent.retrieve(paymentIntentId);
//            } catch (StripeException e) {
//                logger.error("Stripe API error retrieving payment: {}", paymentIntentId, e);
//                throw new RuntimeException("Payment retrieval failed: " + e.getMessage());
//            }
//        }
//
//        public Refund createRefund(String paymentIntentId, BigDecimal amount) throws StripeException {
//            try {
//                long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
//
//                Refund.RefundCreateParams params = Refund.RefundCreateParams.builder()
//                        .setPaymentIntent(paymentIntentId)
//                        .setAmount(amountCents)
//                        .build();
//
//                return Refund.create(params);
//            } catch (StripeException e) {
//                logger.error("Stripe API error creating refund: {}", paymentIntentId, e);
//                throw new RuntimeException("Refund processing failed: " + e.getMessage());
//            }
//        }
//
//        public boolean verifyPayment(String paymentIntentId) {
//            try {
//                PaymentIntent paymentIntent = getPaymentIntent(paymentIntentId);
//                return "succeeded".equals(paymentIntent.getStatus());
//            } catch (StripeException e) {
//                logger.error("Error verifying payment: {}", paymentIntentId, e);
//                return false;
//            }
//        }
//
//        public Map<String, Object> getPaymentStatus(String paymentIntentId) {
//            try {
//                PaymentIntent paymentIntent = getPaymentIntent(paymentIntentId);
//
//                Map<String, Object> status = new HashMap<>();
//                status.put("status", paymentIntent.getStatus());
//                status.put("amount", paymentIntent.getAmount());
//                status.put("currency", paymentIntent.getCurrency());
//                status.put("created", paymentIntent.getCreated());
//
//                return status;
//            } catch (StripeException e) {
//                logger.error("Error getting payment status: {}", paymentIntentId, e);
//                throw new RuntimeException("Failed to get payment status: " + e.getMessage());
//            }
//        }
//
//        public boolean verifyWebhookSignature(String payload, String sigHeader) {
//            // Implement webhook signature verification for production
//            // This is crucial for security
//            logger.info("Webhook signature verification - implement for production");
//            return true; // Placeholder - implement proper verification
//        }
//
//        public void handlePaymentSuccess(String paymentIntentId) {
//            try {
//                PaymentIntent paymentIntent = getPaymentIntent(paymentIntentId);
//                String userId = paymentIntent.getMetadata().get("user_id");
//
//                if ("succeeded".equals(paymentIntent.getStatus())) {
//                    logger.info("Payment succeeded: {} for user: {}", paymentIntentId, userId);
//                    // Update your database to mark payment as verified
//                    // This would typically update the TestAttempt entity
//                }
//            } catch (StripeException e) {
//                logger.error("Error handling payment success: {}", paymentIntentId, e);
//            }
//        }
//    }
//}



package org.harry.ascholar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${stripe.secret.key:sk_test_defaultKey}")
    private String secretKey;

    @Value("${stripe.webhook.secret:whsec_defaultSecret}")
    private String webhookSecret;

    // Flag to track if Stripe is configured
    private boolean isStripeConfigured = false;

    public PaymentService() {
        // Initialize Stripe in constructor
        initializeStripe();
    }

    private void initializeStripe() {
        try {
            if (secretKey != null && !secretKey.startsWith("sk_test_defaultKey")) {
                // Stripe.apiKey = secretKey; // Uncomment when you add Stripe dependency
                isStripeConfigured = true;
                logger.info("Stripe payment service initialized successfully");
            } else {
                logger.warn("Stripe secret key not configured. Using mock payment service.");
                isStripeConfigured = false;
            }
        } catch (Exception e) {
            logger.warn("Failed to initialize Stripe. Using mock payment service.", e);
            isStripeConfigured = false;
        }
    }

    public Map<String, Object> createPaymentForTest(BigDecimal amount, String description, Long userId) {
        logger.info("Creating payment for test - Amount: {}, Description: {}, User: {}", amount, description, userId);

        try {
            if (isStripeConfigured) {
                return createStripePaymentIntent(amount, description, userId);
            } else {
                return createMockPaymentIntent(amount, description, userId);
            }
        } catch (Exception e) {
            logger.error("Payment creation failed for user: {}", userId, e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }

    private Map<String, Object> createStripePaymentIntent(BigDecimal amount, String description, Long userId) {
        // This would be the actual Stripe implementation
        // For now, we'll return mock data since Stripe dependency isn't added

        Map<String, Object> response = new HashMap<>();
        response.put("clientSecret", "pi_mock_client_secret_" + System.currentTimeMillis());
        response.put("paymentIntentId", "pi_mock_" + System.currentTimeMillis());
        response.put("amount", amount);
        response.put("status", "requires_payment_method");
        response.put("currency", "usd");
        response.put("mock", true); // Indicate this is mock data

        logger.info("Mock Stripe payment intent created for user: {}", userId);
        return response;
    }

    private Map<String, Object> createMockPaymentIntent(BigDecimal amount, String description, Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("paymentIntentId", "mock_pi_" + System.currentTimeMillis());
        response.put("amount", amount);
        response.put("status", "succeeded"); // Mock successful payment
        response.put("currency", "usd");
        response.put("mock", true);
        response.put("message", "Mock payment service - payment automatically approved");

        logger.info("Mock payment created for user: {}", userId);
        return response;
    }

    public boolean confirmPayment(String paymentIntentId) {
        logger.info("Confirming payment: {}", paymentIntentId);

        try {
            if (isStripeConfigured) {
                return confirmStripePayment(paymentIntentId);
            } else {
                return confirmMockPayment(paymentIntentId);
            }
        } catch (Exception e) {
            logger.error("Payment confirmation failed: {}", paymentIntentId, e);
            return false;
        }
    }

    private boolean confirmStripePayment(String paymentIntentId) {
        // Mock Stripe confirmation
        logger.info("Mock Stripe payment confirmation: {}", paymentIntentId);
        return true;
    }

    private boolean confirmMockPayment(String paymentIntentId) {
        logger.info("Mock payment confirmation: {}", paymentIntentId);
        return true; // Always succeed in mock mode
    }

    public Map<String, Object> getPaymentStatus(String paymentIntentId) {
        logger.debug("Getting payment status: {}", paymentIntentId);

        Map<String, Object> status = new HashMap<>();

        if (paymentIntentId.startsWith("mock_")) {
            status.put("status", "succeeded");
            status.put("amount", 9999); // Mock amount
            status.put("currency", "usd");
            status.put("mock", true);
        } else {
            status.put("status", "unknown");
            status.put("message", "Payment service not fully configured");
        }

        return status;
    }

    public boolean verifyPayment(String paymentIntentId) {
        logger.debug("Verifying payment: {}", paymentIntentId);

        // For mock payments, always return true
        // For real Stripe payments, you would check the actual status
        return paymentIntentId != null &&
                (paymentIntentId.startsWith("mock_") || paymentIntentId.startsWith("pi_mock_"));
    }

    public boolean refundPayment(String paymentIntentId, BigDecimal amount) {
        logger.info("Processing refund - Payment: {}, Amount: {}", paymentIntentId, amount);

        try {
            if (isStripeConfigured) {
                return processStripeRefund(paymentIntentId, amount);
            } else {
                return processMockRefund(paymentIntentId, amount);
            }
        } catch (Exception e) {
            logger.error("Refund processing failed: {}", paymentIntentId, e);
            return false;
        }
    }

    private boolean processStripeRefund(String paymentIntentId, BigDecimal amount) {
        // Mock Stripe refund
        logger.info("Mock Stripe refund processed: {}", paymentIntentId);
        return true;
    }

    private boolean processMockRefund(String paymentIntentId, BigDecimal amount) {
        logger.info("Mock refund processed: {} for amount: {}", paymentIntentId, amount);
        return true;
    }

    public boolean isStripeConfigured() {
        return isStripeConfigured;
    }

    public void updateStripeConfig(String newSecretKey) {
        this.secretKey = newSecretKey;
        initializeStripe();
        logger.info("Stripe configuration updated");
    }

    // Utility method to validate payment amount
    public boolean isValidPaymentAmount(BigDecimal amount) {
        return amount != null &&
                amount.compareTo(BigDecimal.ZERO) > 0 &&
                amount.compareTo(new BigDecimal("10000")) <= 0; // Max $10,000
    }

    // Method to calculate platform fee
    public BigDecimal calculatePlatformFee(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;

        // 10% platform fee
        BigDecimal feePercentage = new BigDecimal("0.10");
        return amount.multiply(feePercentage).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // Method to calculate amount after platform fee
    public BigDecimal calculateNetAmount(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;

        BigDecimal platformFee = calculatePlatformFee(amount);
        return amount.subtract(platformFee).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public boolean verifyWebhookSignature(String payload, String sigHeader) {
        logger.info("Webhook signature verification - implement for production");

        // In production, implement proper Stripe webhook signature verification
        // For now, return true for development
        return true;
    }

    public void handlePaymentSuccess(String paymentIntentId) {
        logger.info("Handling payment success for: {}", paymentIntentId);

        // This would typically:
        // 1. Verify the payment with Stripe
        // 2. Update your database records
        // 3. Send confirmation emails
        // 4. Trigger any post-payment actions

        if (verifyPayment(paymentIntentId)) {
            logger.info("Payment successfully processed: {}", paymentIntentId);
            // Add your business logic here
        } else {
            logger.warn("Payment verification failed: {}", paymentIntentId);
        }
    }

    // Method for tutor payouts (for future use)
    public Map<String, Object> createTutorPayout(String tutorStripeAccountId, BigDecimal amount, String description) {
        logger.info("Creating tutor payout - Account: {}, Amount: {}", tutorStripeAccountId, amount);

        Map<String, Object> response = new HashMap<>();
        response.put("payoutId", "po_mock_" + System.currentTimeMillis());
        response.put("amount", amount);
        response.put("status", "pending");
        response.put("currency", "usd");
        response.put("mock", true);
        response.put("message", "Mock tutor payout created");

        logger.info("Mock tutor payout created for account: {}", tutorStripeAccountId);
        return response;
    }
}
