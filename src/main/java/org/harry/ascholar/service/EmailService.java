package org.harry.ascholar.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.harry.ascholar.data.models.Test;
import org.harry.ascholar.data.models.TestAttempt;
import org.harry.ascholar.data.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

    @Service
    public class EmailService {
        private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

        private final JavaMailSender mailSender;
        private final TemplateEngine templateEngine;

        @Value("${spring.mail.username}")
        private String fromEmail;

        @Value("${app.base-url:http://localhost:8080}")
        private String baseUrl;

        @Value("${app.name:EduLink}")
        private String appName;

        @Value("${spring.mail.from-name:EduLink Support}")
        private String fromName;

        @Autowired
        public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
            this.mailSender = mailSender;
            this.templateEngine = templateEngine;
        }

        @Async
        public void sendTestRegistrationConfirmation(String toEmail, Test test, TestAttempt attempt) {
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("testName", test.getTitle());
                variables.put("attemptId", attempt.getId());
                variables.put("testDate", test.getStartTime() != null ? test.getStartTime().toString() : "To be scheduled");
                variables.put("appName", appName);
                variables.put("dashboardUrl", baseUrl + "/student/tests/" + attempt.getId());

                String subject = "Test Registration Confirmed - " + test.getTitle();
                String content = renderEmailTemplate("test-registration-confirmation", variables);

                sendEmail(toEmail, subject, content, true);

            } catch (Exception e) {
                logger.error("Failed to send test registration confirmation email to: {}", toEmail, e);
            }
        }

        @Async
        public void sendTestCompletionConfirmation(String toEmail, Test test, TestAttempt attempt) {
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("testName", test.getTitle());
                variables.put("score", attempt.getScore() != null ? attempt.getScore().toString() : "Pending");
                variables.put("completedAt", attempt.getCompletedAt() != null ? attempt.getCompletedAt().toString() : "Just now");
                variables.put("appName", appName);
                variables.put("resultsUrl", baseUrl + "/student/results/" + attempt.getId());

                String subject = "Test Completed - " + test.getTitle();
                String content = renderEmailTemplate("test-completion-confirmation", variables);

                sendEmail(toEmail, subject, content, true);

            } catch (Exception e) {
                logger.error("Failed to send test completion email to: {}", toEmail, e);
            }
        }


        @Async
        public CompletableFuture<Boolean> sendVerificationEmail(User user) {
            try {
                String verificationToken = generateVerificationToken(); // You'd implement this
                String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + verificationToken;

                Map<String, Object> variables = new HashMap<>();
                variables.put("name", user.getProfile().getFirstName());
                variables.put("verificationUrl", verificationUrl);
                variables.put("appName", appName);
                variables.put("supportEmail", fromEmail);

                String subject = "Verify Your Email Address - " + appName;
                String content = renderEmailTemplate("email-verification", variables);

                return sendEmail(user.getEmail(), subject, content, true);

            } catch (Exception e) {
                logger.error("Failed to send verification email to: {}", user.getEmail(), e);
                return CompletableFuture.completedFuture(false);
            }
        }

        @Async
        public CompletableFuture<Boolean> sendPasswordResetEmail(User user, String resetToken) {
            try {
                String resetUrl = baseUrl + "/api/auth/reset-password?token=" + resetToken;

                Map<String, Object> variables = new HashMap<>();
                variables.put("name", user.getProfile().getFirstName());
                variables.put("resetUrl", resetUrl);
                variables.put("appName", appName);
                variables.put("expiryHours", 24); // Token expiry

                String subject = "Password Reset Request - " + appName;
                String content = renderEmailTemplate("password-reset", variables);

                return sendEmail(user.getEmail(), subject, content, true);

            } catch (Exception e) {
                logger.error("Failed to send password reset email to: {}", user.getEmail(), e);
                return CompletableFuture.completedFuture(false);
            }
        }

        @Async
        public CompletableFuture<Boolean> sendTestConfirmationEmail(User user, String testName, String testDate) {
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("name", user.getProfile().getFirstName());
                variables.put("testName", testName);
                variables.put("testDate", testDate);
                variables.put("appName", appName);
                variables.put("dashboardUrl", baseUrl + "/dashboard");

                String subject = "Test Registration Confirmation - " + testName;
                String content = renderEmailTemplate("test-confirmation", variables);

                return sendEmail(user.getEmail(), subject, content, true);

            } catch (Exception e) {
                logger.error("Failed to send test confirmation email to: {}", user.getEmail(), e);
                return CompletableFuture.completedFuture(false);
            }
        }

        @Async
        public CompletableFuture<Boolean> sendTestResultsEmail(User user, String testName, BigDecimal score, String feedback) {
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("name", user.getProfile().getFirstName());
                variables.put("testName", testName);
                variables.put("score", score);
                variables.put("feedback", feedback);
                variables.put("appName", appName);
                variables.put("resultsUrl", baseUrl + "/test-results/" + testName);

                String subject = "Test Results Available - " + testName;
                String content = renderEmailTemplate("test-results", variables);

                return sendEmail(user.getEmail(), subject, content, true);

            } catch (Exception e) {
                logger.error("Failed to send test results email to: {}", user.getEmail(), e);
                return CompletableFuture.completedFuture(false);
            }
        }

        @Async
        public CompletableFuture<Boolean> sendTutorApplicationEmail(User tutor, String applicationId) {
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("name", tutor.getProfile().getFirstName());
                variables.put("applicationId", applicationId);
                variables.put("appName", appName);
                variables.put("statusUrl", baseUrl + "/tutor/application-status");

                String subject = "Tutor Application Received - " + appName;
                String content = renderEmailTemplate("tutor-application", variables);

                return sendEmail(tutor.getEmail(), subject, content, true);

            } catch (Exception e) {
                logger.error("Failed to send tutor application email to: {}", tutor.getEmail(), e);
                return CompletableFuture.completedFuture(false);
            }
        }

        private CompletableFuture<Boolean> sendEmail(String toEmail, String subject, String content, boolean isHtml) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                    helper.setFrom(fromEmail, fromName);
                    helper.setTo(toEmail);
                    helper.setSubject(subject);
                    helper.setText(content, isHtml);

                    mailSender.send(message);

                    logger.info("Email sent successfully to: {}", toEmail);
                    return true;

                } catch (MessagingException | UnsupportedEncodingException e) {
                    logger.error("Failed to send email to: {}", toEmail, e);
                    return false;
                }
            });
        }

        private String renderEmailTemplate(String templateName, Map<String, Object> variables) {
            Context context = new Context();
            variables.forEach(context::setVariable);

            return templateEngine.process("emails/" + templateName, context);
        }

        private String generateVerificationToken() {
            // Implement your token generation logic
            return java.util.UUID.randomUUID().toString();
        }

        // Utility method for simple text emails (without templates)
        @Async
        public void sendSimpleEmail(String toEmail, String subject, String text) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

                helper.setFrom(fromEmail, fromName);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(text, false);

                mailSender.send(message);
                logger.info("Simple email sent to: {}", toEmail);

            } catch (MessagingException | UnsupportedEncodingException e) {
                logger.error("Failed to send simple email to: {}", toEmail, e);
            }
        }
    }

