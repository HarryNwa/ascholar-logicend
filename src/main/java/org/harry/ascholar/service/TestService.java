package org.harry.ascholar.service;

import org.harry.ascholar.data.models.Test;
import org.harry.ascholar.data.repo.TestRepository;
import org.harry.ascholar.exceptions.ResourceNotFoundException;
import org.harry.ascholar.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestService {

        private static final Logger logger = LoggerFactory.getLogger(TestService.class);

        private final TestRepository testRepository;
        private final ValidationUtils validationUtils;

        @Autowired
        public TestService(TestRepository testRepository, ValidationUtils validationUtils) {
            this.testRepository = testRepository;
            this.validationUtils = validationUtils;
        }

        public List<Test> getAllActiveTests() {
            logger.debug("Fetching all active tests");
            return testRepository.findByIsActiveTrue();
        }

        public Page<Test> getAllTests(Pageable pageable) {
            logger.debug("Fetching all tests with pagination: {}", pageable);
            return testRepository.findAll(pageable);
        }

        public Test getTestById(Long id) {
            logger.debug("Fetching test by ID: {}", id);
            return testRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("Test not found with ID: {}", id);
                        return new ResourceNotFoundException("Test", "id", id);
                    });
        }

        @Transactional
        public Test createTest(Test test) {
            logger.info("Creating new test: {}", test.getTitle());

            validateTest(test);

            test.setIsActive(true);
            test.setCreatedAt(LocalDateTime.now());
            test.setUpdatedAt(LocalDateTime.now());

            Test createdTest = testRepository.save(test);
            logger.info("Test created successfully with ID: {}", createdTest.getId());

            return createdTest;
        }

        @Transactional
        public Test updateTest(Long testId, Test testUpdates) {
            logger.info("Updating test with ID: {}", testId);

            Test existingTest = getTestById(testId);

            // Update allowed fields
            if (testUpdates.getTitle() != null) {
                validationUtils.validateNotBlank(testUpdates.getTitle(), "Test title");
                existingTest.setTitle(testUpdates.getTitle().trim());
            }

            if (testUpdates.getDescription() != null) {
                existingTest.setDescription(testUpdates.getDescription());
            }

            if (testUpdates.getDuration() != null) {
                validationUtils.validatePositiveNumber(testUpdates.getDuration().getSeconds(), "Duration");
                existingTest.setDuration(testUpdates.getDuration());
            }

            if (testUpdates.getFee() != null) {
                validationUtils.validateNonNegativeNumber(testUpdates.getFee(), "Fee");
                existingTest.setFee(testUpdates.getFee());
            }

            existingTest.setUpdatedAt(LocalDateTime.now());

            Test updatedTest = testRepository.save(existingTest);
            logger.info("Test updated successfully: {}", testId);

            return updatedTest;
        }

        @Transactional
        public void deleteTest(Long testId) {
            logger.info("Deleting test with ID: {}", testId);

            Test test = getTestById(testId);
            testRepository.delete(test);

            logger.info("Test deleted: {}", testId);
        }

        @Transactional
        public Test deactivateTest(Long testId) {
            logger.info("Deactivating test with ID: {}", testId);

            Test test = getTestById(testId);
            test.setIsActive(false);
            test.setUpdatedAt(LocalDateTime.now());

            Test deactivatedTest = testRepository.save(test);
            logger.info("Test deactivated: {}", testId);

            return deactivatedTest;
        }

        @Transactional
        public Test activateTest(Long testId) {
            logger.info("Activating test with ID: {}", testId);

            Test test = getTestById(testId);
            test.setIsActive(true);
            test.setUpdatedAt(LocalDateTime.now());

            Test activatedTest = testRepository.save(test);
            logger.info("Test activated: {}", testId);

            return activatedTest;
        }

        @Transactional
        public Test updateTestFee(Long testId, BigDecimal newFee) {
            logger.info("Updating fee for test ID: {} to {}", testId, newFee);

            validationUtils.validateNonNegativeNumber(newFee, "Test fee");

            Test test = getTestById(testId);
            test.setFee(newFee);
            test.setUpdatedAt(LocalDateTime.now());

            Test updatedTest = testRepository.save(test);
            logger.info("Test fee updated: {} to {}", testId, newFee);

            return updatedTest;
        }

        @Transactional
        public Test updateTestDuration(Long testId, Duration newDuration) {
            logger.info("Updating duration for test ID: {} to {}", testId, newDuration);

            validationUtils.validatePositiveNumber(newDuration.getSeconds(), "Test duration");

            Test test = getTestById(testId);
            test.setDuration(newDuration);
            test.setUpdatedAt(LocalDateTime.now());

            Test updatedTest = testRepository.save(test);
            logger.info("Test duration updated: {} to {}", testId, newDuration);

            return updatedTest;
        }

        public List<Test> searchTests(String keyword) {
            logger.debug("Searching tests with keyword: {}", keyword);

            if (keyword == null || keyword.trim().isEmpty()) {
                return testRepository.findByIsActiveTrue();
            }

            return testRepository.findByTitleContainingIgnoreCaseAndIsActiveTrue(keyword.trim());
        }

        public long getTotalTestCount() {
            return testRepository.count();
        }

        public long getActiveTestCount() {
            return testRepository.countByIsActiveTrue();
        }

        private void validateTest(Test test) {
            validationUtils.validateNotBlank(test.getTitle(), "Test title");
            validationUtils.validateMinLength(test.getTitle(), 3, "Test title");
            validationUtils.validateMaxLength(test.getTitle(), 255, "Test title");

            validationUtils.validateNotNull(test.getDuration(), "Test duration");
            validationUtils.validatePositiveNumber(test.getDuration().getSeconds(), "Duration");

            validationUtils.validateNotNull(test.getFee(), "Test fee");
            validationUtils.validateNonNegativeNumber(test.getFee(), "Fee");

            if (test.getDescription() != null) {
                validationUtils.validateMaxLength(test.getDescription(), 1000, "Test description");
            }
        }
    }

