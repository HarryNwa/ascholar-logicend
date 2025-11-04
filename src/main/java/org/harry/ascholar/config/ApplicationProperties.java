package org.harry.ascholar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Duration;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    @Data
    public static class Jwt {
        @NotBlank
        private String secretKey = "your-super-secure-jwt-secret-key-minimum-256-bits-change-in-production";

        @NotNull
        @Positive
        private long expiration = 86400000; // 24 hours in milliseconds

        @NotNull
        @Positive
        private long refreshExpiration = 604800000; // 7 days in milliseconds
    }

    @Data
    public static class Security {
        @NotNull
        private Duration authTokenValidity = Duration.ofHours(24);

        @Positive
        private int maxLoginAttempts = 5;

        @NotNull
        private Duration loginLockoutDuration = Duration.ofMinutes(30);

        @Positive
        private int passwordMinLength = 8;

        @Positive
        private int passwordMaxLength = 128;

        private boolean requireSpecialChar = true;
        private boolean requireUppercase = true;
        private boolean requireNumber = true;
    }

    @Data
    public static class Test {
        @NotNull
        private Duration autoSubmitCheckInterval = Duration.ofMinutes(1);

        @NotNull
        private BigDecimal highPerformerThreshold = BigDecimal.valueOf(80);

        @Positive
        private int maxTabSwitches = 3;

        @NotNull
        private Duration answerSubmissionRateLimit = Duration.ofSeconds(2);

        @Positive
        private int answerSubmissionRateLimitCount = 10; // requests per time window
    }

    @Data
    public static class Email {
        @NotBlank
        private String fromAddress = "noreply@ascholar.com";

        @NotBlank
        private String supportEmail = "support@ascholar.com";
    }

    @Data
    public static class Cache {
        @NotNull
        private Duration testAttemptTtl = Duration.ofMinutes(30);

        @NotNull
        private Duration userProfileTtl = Duration.ofHours(1);

        @NotNull
        private Duration rateLimitTtl = Duration.ofHours(1);
    }

    @Data
    public static class Payment {
        @NotBlank
        private String currency = "USD";

        @Positive
        private BigDecimal processingFeeRate = BigDecimal.valueOf(2.9); // 2.9%

        @NotNull
        @Positive
        private BigDecimal fixedFee = BigDecimal.valueOf(0.30); // $0.30
    }

    private Jwt jwt = new Jwt();
    private Security security = new Security();
    private Test test = new Test();
    private Email email = new Email();
    private Cache cache = new Cache();
    private Payment payment = new Payment();
}