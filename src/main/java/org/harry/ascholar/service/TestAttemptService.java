package org.harry.ascholar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.harry.ascholar.config.ApplicationProperties;
import org.harry.ascholar.data.models.*;
import org.harry.ascholar.data.enums.TestStatus;
import org.harry.ascholar.data.repo.ProfileRepository;
import org.harry.ascholar.data.repo.TestAttemptRepository;
import org.harry.ascholar.data.repo.TestAnswerRepository;
import org.harry.ascholar.dto.SubmitAnswerRequest;
import org.harry.ascholar.dto.SubmitTestRequest;
import org.harry.ascholar.exceptions.*;
import org.harry.ascholar.exceptions.SecurityException;
import org.harry.ascholar.utils.ValidationUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestAttemptService {
    private final TestAttemptRepository testAttemptRepository;
    private final TestAnswerRepository testAnswerRepository;
    private final ProfileRepository profileRepository;
    private final TestService testService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final RateLimitService rateLimitService;
    private final SecurityService securityService;
    private final ApplicationProperties properties;

    private final Map<String, LocalDateTime> answerSubmissionCache = new ConcurrentHashMap<>();

    private static final String TEST_ATTEMPT_CACHE = "testAttempts";
    private static final String USER_ATTEMPTS_CACHE = "userAttempts";

    @Transactional
    @CacheEvict(value = {USER_ATTEMPTS_CACHE}, key = "#userId")
    public TestAttempt registerForTest(Long testId, Long userId) {
        log.info("Registering user {} for test {}", userId, testId);

        // Validate inputs
        ValidationUtils.validateId(testId, "testId");
        ValidationUtils.validateId(userId, "userId");

        Test test = testService.getTestById(testId);
        User user = userService.getUserById(userId);
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new StudentProfileNotFoundException(userId));

        // Check for existing active attempts
        Optional<TestAttempt> existingAttempt = testAttemptRepository
                .findByTestIdAndUserIdAndStatusIn(testId, userId,
                        List.of(TestStatus.REGISTERED, TestStatus.IN_PROGRESS));

        if (existingAttempt.isPresent()) {
            throw new DuplicateTestAttemptException(testId, userId);
        }

        // Validate test availability
        if (!isTestRegistrationOpen(test)) {
            throw new TestRegistrationClosedException(testId, test.getRegistrationDeadline());
        }

        // Validate user can take tests
        if (!user.canTakeTests()) {
            throw new InvalidTestStateException("User is not eligible to take tests");
        }

        TestAttempt attempt = createNewTestAttempt(test, user);

        try {
            processPayment(attempt, test, user);
            TestAttempt savedAttempt = testAttemptRepository.save(attempt);

            // Audit and notifications
            auditService.logTestRegistration(userId, testId, savedAttempt.getId());
            sendRegistrationNotifications(user, test, savedAttempt);

            log.info("User {} successfully registered for test {} with attempt ID: {}",
                    userId, testId, savedAttempt.getId());
            return savedAttempt;

        } catch (Exception e) {
            log.error("Payment processing failed for test: {} user: {}", testId, userId, e);
            auditService.logPaymentFailure(userId, testId, e.getMessage());
            if (e instanceof PaymentProcessingException) {
                throw e;
            }
            throw new PaymentProcessingException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    @PreAuthorize("@securityService.canAccessTestAttempt(#attemptId, #userId)")
    public TestAttempt startTest(Long attemptId, Long userId, String ipAddress, String userAgent) {
        log.info("Starting test attempt: {} for user: {}", attemptId, userId);

        ValidationUtils.validateId(attemptId, "attemptId");
        ValidationUtils.validateId(userId, "userId");
        ValidationUtils.validateIpAddress(ipAddress);
        ValidationUtils.validateUserAgent(userAgent);

        TestAttempt attempt = getTestAttemptById(attemptId);

        // Validate preconditions
        validateTestStartPreconditions(attempt, userId);

        attempt.setStatus(TestStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setUpdatedAt(LocalDateTime.now());
        attempt.setIpAddress(ipAddress);
        attempt.setUserAgent(userAgent);
        attempt.setCurrentQuestionIndex(0);

        TestAttempt startedAttempt = testAttemptRepository.save(attempt);

        // Audit and security logging
        auditService.logTestStart(userId, attemptId, ipAddress, userAgent);
        log.info("Test started - attempt: {}, user: {}, IP: {}", attemptId, userId, ipAddress);

        return startedAttempt;
    }

    @Transactional
    @PreAuthorize("@securityService.canAccessTestAttempt(#request.attemptId(), #request.userId())")
    public TestAnswer submitAnswer(SubmitAnswerRequest request) {
        log.debug("Submitting answer for attempt: {}, question: {}",
                request.attemptId(), request.questionId());

        // Validate request
        request.validate();

        // Rate limiting
        String rateLimitKey = String.format("answer:%d:%d",
                request.attemptId(), request.userId());
        if (!rateLimitService.tryAcquire(rateLimitKey,
                properties.getTest().getAnswerSubmissionRateLimit())) {
            throw new RateLimitExceededException("Answer submission rate limit exceeded");
        }

        TestAttempt attempt = getTestAttemptById(request.attemptId());

        // Validate test state
        if (attempt.getStatus() != TestStatus.IN_PROGRESS) {
            throw new InvalidTestStateException("Cannot submit answer for test in status: " + attempt.getStatus());
        }

        if (isTimeExpired(attempt)) {
            Duration elapsed = Duration.between(attempt.getStartedAt(), LocalDateTime.now());
            Duration allowedDuration = attempt.getTest().getDuration();
            throw new TestTimeExpiredException(request.attemptId(), allowedDuration, elapsed);
        }

        TestAnswer testAnswer = createOrUpdateTestAnswer(request, attempt);
        TestAnswer savedAnswer = testAnswerRepository.save(testAnswer);

        attempt.setUpdatedAt(LocalDateTime.now());
        testAttemptRepository.save(attempt);

        auditService.logAnswerSubmission(request.userId(), request.attemptId(),
                request.questionId(), request.timeSpentSeconds());

        log.debug("Answer submitted successfully for attempt: {}, question: {}",
                request.attemptId(), request.questionId());

        return savedAnswer;
    }

    @Transactional
    @PreAuthorize("@securityService.canAccessTestAttempt(#request.attemptId(), #request.userId())")
    @Caching(evict = {
            @CacheEvict(value = TEST_ATTEMPT_CACHE, key = "#request.attemptId()"),
            @CacheEvict(value = USER_ATTEMPTS_CACHE, key = "#request.userId()")
    })
    public TestAttempt submitTest(SubmitTestRequest request) {
        log.info("Submitting test attempt: {} for user: {}", request.attemptId(), request.userId());

        request.validate();

        TestAttempt attempt = getTestAttemptById(request.attemptId());

        if (attempt.getStatus() != TestStatus.IN_PROGRESS) {
            throw new InvalidTestStateException("Test cannot be submitted in current status: " + attempt.getStatus());
        }

        // Check if time expired but allow forced submission
        if (isTimeExpired(attempt) && !request.isForcedSubmission()) {
            Duration elapsed = Duration.between(attempt.getStartedAt(), LocalDateTime.now());
            Duration allowedDuration = attempt.getTest().getDuration();
            throw new TestTimeExpiredException(request.attemptId(), allowedDuration, elapsed);
        }

        attempt.setStatus(TestStatus.COMPLETED);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setTimeSpent(calculateTimeSpent(attempt));
        attempt.setUpdatedAt(LocalDateTime.now());

        // Calculate score with proper error handling
        calculateAndSetScore(attempt);

        TestAttempt submittedAttempt = testAttemptRepository.save(attempt);

        // Update student profile and send notifications
        updateStudentProfileWithTestResult(attempt);
        sendCompletionNotifications(attempt);

        // Notify universities for high performers
        if (isHighPerformer(attempt)) {
            notifyUniversitiesOfHighPerformer(attempt);
        }

        auditService.logTestCompletion(request.userId(), request.attemptId(), attempt.getScore());
        log.info("Test submitted - attempt: {}, user: {}, score: {}",
                request.attemptId(), request.userId(), attempt.getScore());

        return submittedAttempt;
    }

    @Scheduled(fixedDelayString = "${app.test.auto-submit-check-interval:60000}")
    @Transactional
    public void autoSubmitExpiredTests() {
        log.debug("Checking for expired tests to auto-submit");

        List<TestAttempt> inProgressAttempts = testAttemptRepository
                .findByStatus(TestStatus.IN_PROGRESS);

        if (inProgressAttempts.isEmpty()) {
            return;
        }

        int autoSubmittedCount = 0;
        List<TestAttempt> expiredAttempts = inProgressAttempts.stream()
                .filter(this::isTimeExpired)
                .collect(Collectors.toList());

        for (TestAttempt attempt : expiredAttempts) {
            try {
                Duration elapsed = Duration.between(attempt.getStartedAt(), LocalDateTime.now());
                Duration allowedDuration = attempt.getTest().getDuration();

                log.info("Auto-submitting expired attempt {} - Allowed: {}, Elapsed: {}",
                        attempt.getId(), allowedDuration, elapsed);

                autoSubmitAttempt(attempt);
                autoSubmittedCount++;

            } catch (Exception e) {
                log.error("Failed to auto-submit test attempt: {}", attempt.getId(), e);
                auditService.logAutoSubmitFailure(attempt.getId(), e.getMessage());
            }
        }

        if (autoSubmittedCount > 0) {
            log.info("Auto-submitted {} expired test attempts", autoSubmittedCount);
            auditService.logAutoSubmitBatch(autoSubmittedCount);
        }
    }

    @Cacheable(value = TEST_ATTEMPT_CACHE, key = "#attemptId", unless = "#result == null")
    @PreAuthorize("@securityService.canAccessTestAttempt(#attemptId, authentication.principal.id)")
    public TestAttempt getTestAttemptById(Long attemptId) {
        ValidationUtils.validateId(attemptId, "attemptId");

        return testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new TestAttemptNotFoundException(attemptId));
    }

    @Cacheable(value = USER_ATTEMPTS_CACHE, key = "#userId")
    @PreAuthorize("#userId == authentication.principal.id")
    public List<TestAttempt> getUserAttempts(Long userId) {
        ValidationUtils.validateId(userId, "userId");

        return testAttemptRepository.findByUserId(userId);
    }

    public Page<TestAttempt> getUserAttempts(Long userId, Pageable pageable) {
        ValidationUtils.validateId(userId, "userId");
        return testAttemptRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Duration getRemainingTime(Long attemptId) {
        TestAttempt attempt = getTestAttemptById(attemptId);
        return attempt.getRemainingTime();
    }

    public boolean hasSuspiciousActivity(Long attemptId) {
        TestAttempt attempt = getTestAttemptById(attemptId);
        return attempt.hasSuspiciousActivity();
    }

    // Private helper methods
    private TestAttempt createNewTestAttempt(Test test, User user) {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUser(user);
        attempt.setStatus(TestStatus.REGISTERED);
        attempt.setCreatedAt(LocalDateTime.now());
        attempt.setUpdatedAt(LocalDateTime.now());
        attempt.setTabSwitchCount(0);
        attempt.setFullscreenExitCount(0);
        attempt.setPaymentVerified(false);
        attempt.setPaymentAmount(test.getFee());
        return attempt;
    }

    private void processPayment(TestAttempt attempt, Test test, User user) {
        try {
            Map<String, Object> paymentResult = paymentService.createPaymentForTest(
                    test.getFee(), test.getTitle(), user.getId());

            attempt.setPaymentIntentId((String) paymentResult.get("paymentIntentId"));
            boolean isMockPayment = Boolean.TRUE.equals(paymentResult.get("mock"));
            attempt.setPaymentVerified(isMockPayment);

            if (isMockPayment) {
                emailService.sendTestRegistrationConfirmation(user.getEmail(), test, attempt);
            }

        } catch (Exception e) {
            throw new PaymentProcessingException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    private void validateTestStartPreconditions(TestAttempt attempt, Long userId) {
        if (!attempt.getUser().getId().equals(userId)) {
            throw new SecurityException("User not authorized to access this test attempt");
        }

        if (!Boolean.TRUE.equals(attempt.getPaymentVerified())) {
            throw new PaymentNotVerifiedException(attempt.getId(), attempt.getPaymentIntentId());
        }

        if (attempt.getStatus() != TestStatus.REGISTERED) {
            throw new InvalidTestStateException("Test cannot be started in current status: " + attempt.getStatus());
        }

        if (!isTestAvailable(attempt.getTest())) {
            throw new TestNotAvailableException(attempt.getTest().getId(),
                    attempt.getTest().getStartTime(), attempt.getTest().getEndTime());
        }

        // Additional security checks
        if (attempt.getStartedAt() != null) {
            throw new InvalidTestStateException("Test has already been started");
        }
    }

    private TestAnswer createOrUpdateTestAnswer(SubmitAnswerRequest request, TestAttempt attempt) {
        return testAnswerRepository
                .findByAttemptIdAndQuestionId(request.attemptId(), request.questionId())
                .map(existingAnswer -> updateExistingAnswer(existingAnswer, request))
                .orElseGet(() -> createNewTestAnswer(request, attempt));
    }

    private TestAnswer updateExistingAnswer(TestAnswer existingAnswer, SubmitAnswerRequest request) {
        existingAnswer.setAnswer(request.answer());
        existingAnswer.setAnsweredAt(LocalDateTime.now());
        existingAnswer.setTimeSpentOnQuestion(request.timeSpentSeconds());
        existingAnswer.setQuestionType(request.questionType());
        existingAnswer.setQuestionPoints(request.questionPoints());
        return existingAnswer;
    }

    private TestAnswer createNewTestAnswer(SubmitAnswerRequest request, TestAttempt attempt) {
        TestAnswer testAnswer = new TestAnswer();
        testAnswer.setAttempt(attempt);
        testAnswer.setQuestionId(request.questionId());
        testAnswer.setAnswer(request.answer());
        testAnswer.setAnsweredAt(LocalDateTime.now());
        testAnswer.setTimeSpentOnQuestion(request.timeSpentSeconds());
        testAnswer.setIsCorrect(null); // Will be set during scoring
        testAnswer.setQuestionType(request.questionType());
        testAnswer.setQuestionPoints(request.questionPoints());
        return testAnswer;
    }

    private void autoSubmitAttempt(TestAttempt attempt) {
        attempt.setStatus(TestStatus.AUTO_SUBMITTED);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setTimeSpent(attempt.getTest().getDuration());
        attempt.setUpdatedAt(LocalDateTime.now());

        calculateAndSetScore(attempt);
        testAttemptRepository.save(attempt);

        updateStudentProfileWithTestResult(attempt);

        notificationService.createNotification(
                attempt.getUser().getId(),
                "TEST_AUTO_SUBMITTED",
                "Test Auto-Submitted",
                String.format("Your test '%s' was automatically submitted due to time expiration",
                        attempt.getTest().getTitle()),
                "/student/tests/" + attempt.getId()
        );

        log.info("Auto-submitted test attempt: {}", attempt.getId());
    }

    private void calculateAndSetScore(TestAttempt attempt) {
        try {
            BigDecimal score = calculateScoreFromAnswers(attempt);
            attempt.setScore(score);
            log.info("Calculated score {} for attempt {}", score, attempt.getId());
        } catch (Exception e) {
            log.error("Error calculating score for attempt {}", attempt.getId(), e);
            attempt.setScore(BigDecimal.ZERO);
            auditService.logScoringError(attempt.getId(), e.getMessage());
        }
    }

    private BigDecimal calculateScoreFromAnswers(TestAttempt attempt) {
        List<TestAnswer> answers = testAnswerRepository.findByAttemptId(attempt.getId());

        if (answers.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long totalQuestions = answers.size();
        long correctAnswers = answers.stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getIsCorrect()))
                .count();

        if (correctAnswers == 0) {
            return BigDecimal.ZERO;
        }

        double percentage = ((double) correctAnswers / totalQuestions) * 100;
        return BigDecimal.valueOf(percentage)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void updateStudentProfileWithTestResult(TestAttempt attempt) {
        try {
            Profile profile = profileRepository.findByUserId(attempt.getUser().getId())
                    .orElseThrow(() -> new StudentProfileNotFoundException(attempt.getUser().getId()));

            // Initialize testScores if null
            if (profile.getTestScores() == null) {
                profile.setTestScores(new HashMap<>());
            }

            profile.getTestScores().put(attempt.getTest().getId(), attempt.getScore());
            profile.setLastTestTaken(LocalDateTime.now());
            profile.setOverallScore(calculateOverallScore(profile));

            profileRepository.save(profile);

            log.debug("Updated profile for user {} with test score: {}",
                    attempt.getUser().getId(), attempt.getScore());

        } catch (Exception e) {
            log.error("Error updating student profile for user {}", attempt.getUser().getId(), e);
            auditService.logProfileUpdateError(attempt.getUser().getId(), e.getMessage());
        }
    }

    private void sendRegistrationNotifications(User user, Test test, TestAttempt attempt) {
        notificationService.createNotification(
                user.getId(),
                "TEST_REGISTERED",
                "Test Registration Confirmed",
                String.format("You have successfully registered for '%s'", test.getTitle()),
                "/student/tests/" + attempt.getId()
        );
    }

    private void sendCompletionNotifications(TestAttempt attempt) {
        // Email notification
        emailService.sendTestCompletionConfirmation(
                attempt.getUser().getEmail(),
                attempt.getTest(),
                attempt
        );

        // In-app notification
        notificationService.createNotification(
                attempt.getUser().getId(),
                "TEST_COMPLETED",
                "Test Completed",
                String.format("Test '%s' completed with score: %s",
                        attempt.getTest().getTitle(),
                        attempt.getScore() != null ? attempt.getScore().toString() + "%" : "Pending"),
                "/student/results/" + attempt.getId()
        );
    }

    private void notifyUniversitiesOfHighPerformer(TestAttempt attempt) {
        try {
            // Get student's full name safely
            String studentFullName = attempt.getUser().getFullName();
            String score = attempt.getScore() != null ? attempt.getScore().toString() + "%" : "Pending";

            // Find university users who should be notified
            List<User> universityUsers = userService.findByRole(org.harry.ascholar.data.enums.UserRole.UNIVERSITY_ADMIN);

            for (User universityUser : universityUsers) {
                notificationService.createNotification(
                        universityUser.getId(),
                        "HIGH_PERFORMER",
                        "High Performing Student",
                        String.format("New high-performing student: %s (Score: %s) in %s",
                                studentFullName, score, attempt.getTest().getCategory()),
                        "/university/talent/" + attempt.getUser().getId()
                );
            }

            log.info("Notified {} universities about high performer: {}",
                    universityUsers.size(), attempt.getUser().getId());

        } catch (Exception e) {
            log.error("Error notifying universities about high performer: {}",
                    attempt.getUser().getId(), e);
        }
    }

    private boolean isTestAvailable(Test test) {
        if (test == null || !Boolean.TRUE.equals(test.getIsActive())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = test.getStartTime() == null || !now.isBefore(test.getStartTime());
        boolean beforeEnd = test.getEndTime() == null || !now.isAfter(test.getEndTime());

        return afterStart && beforeEnd;
    }

    private boolean isTestRegistrationOpen(Test test) {
        if (test == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return test.getRegistrationDeadline() == null || now.isBefore(test.getRegistrationDeadline());
    }

    private boolean isTimeExpired(TestAttempt attempt) {
        if (attempt.getStartedAt() == null) return false;

        LocalDateTime now = LocalDateTime.now();
        Duration elapsed = Duration.between(attempt.getStartedAt(), now);
        Duration testDuration = attempt.getTest().getDuration();

        return elapsed.compareTo(testDuration) > 0;
    }

    private Duration calculateTimeSpent(TestAttempt attempt) {
        if (attempt.getStartedAt() == null) return Duration.ZERO;

        LocalDateTime endTime = attempt.getCompletedAt() != null ?
                attempt.getCompletedAt() : LocalDateTime.now();
        return Duration.between(attempt.getStartedAt(), endTime);
    }

    private boolean isHighPerformer(TestAttempt attempt) {
        return attempt.getScore() != null &&
                attempt.getScore().compareTo(properties.getTest().getHighPerformerThreshold()) >= 0;
    }

    private BigDecimal calculateOverallScore(Profile profile) {
        if (profile.getTestScores() == null || profile.getTestScores().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = profile.getTestScores().values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long scoreCount = profile.getTestScores().values().stream()
                .filter(Objects::nonNull)
                .count();

        if (scoreCount == 0) {
            return BigDecimal.ZERO;
        }

        return sum.divide(BigDecimal.valueOf(scoreCount), 2, RoundingMode.HALF_UP);
    }
}