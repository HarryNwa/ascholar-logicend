package org.harry.ascholar.service;

import org.harry.ascholar.data.models.User;
import org.harry.ascholar.data.models.Profile;
import org.harry.ascholar.data.enums.UserRole;
import org.harry.ascholar.data.repo.UserRepository;
import org.harry.ascholar.dto.UserSignupRequest;
import org.harry.ascholar.exceptions.DuplicateResourceException;
import org.harry.ascholar.exceptions.ResourceNotFoundException;
import org.harry.ascholar.exceptions.UserNotActiveException;
import org.harry.ascholar.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService implements UserDetailsService { // Implement UserDetailsService
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ValidationUtils validationUtils;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       EmailService emailService, ValidationUtils validationUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.validationUtils = validationUtils;
    }

    @Transactional
    public User save(User user) {
        logger.debug("Saving user with ID: {}", user.getId());

        // Ensure updated timestamp is set
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        logger.debug("User saved successfully with ID: {}", savedUser.getId());

        return savedUser;
    }

    // ===== SPRING SECURITY INTEGRATION =====

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user by username (email): {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        if (!user.getIsEnabled()) {
            logger.warn("User account is disabled: {}", email);
            throw new UserNotActiveException("User account is disabled");
        }

        return createUserDetails(user);
    }

    /**
     * Convert our User entity to Spring Security UserDetails
     */
    private UserDetails createUserDetails(User user) {
        // Convert UserRole to Spring Security authority
        String roleAuthority = "ROLE_" + user.getRole().name();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleAuthority);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(authority))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.getIsEnabled())
                .build();
    }

    /**
     * Alternative method to load User entity by email for Spring Security
     */
    @Transactional(readOnly = true)
    public User loadUserEntityByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user entity by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User entity not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        if (!user.getIsEnabled()) {
            logger.warn("User account is disabled: {}", email);
            throw new UserNotActiveException("User account is disabled");
        }

        return user;
    }

    // ===== EXISTING METHODS (with minor improvements) =====

    @Transactional
    public User registerUser(UserSignupRequest signupRequest) {
        logger.info("Attempting to register user with email: {}", signupRequest.getEmail());

        try {
            // Validate input
            validateUserSignupRequest(signupRequest);

            // Check for existing user
            if (userRepository.existsByEmail(signupRequest.getEmail())) {
                logger.warn("Registration failed - email already exists: {}", signupRequest.getEmail());
                throw new DuplicateResourceException("User", "email", signupRequest.getEmail());
            }

            // Create user entity
            User user = createUserFromRequest(signupRequest);

            // Create and link profile
            Profile profile = createProfileFromRequest(signupRequest);
            user.setProfile(profile);

            // Save user
            User savedUser = userRepository.save(user);
            logger.info("User registered successfully with ID: {}", savedUser.getId());

            // Send verification email asynchronously
            sendVerificationEmailAsync(savedUser);

            return savedUser;

        } catch (Exception e) {
            logger.error("User registration failed for email: {}", signupRequest.getEmail(), e);
            throw e; // Re-throw for controller handling
        }
    }

    public User getUserById(Long id) {
        logger.debug("Fetching user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", id);
                    return new ResourceNotFoundException("User", "id", id);
                });
    }

    public Optional<User> findByEmail(String email) {
        logger.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    public User getUserByEmail(String email) {
        logger.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new ResourceNotFoundException("User", "email", email);
                });
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Page<User> getAllUsers(Pageable pageable) {
        logger.debug("Fetching all users with pagination: {}", pageable);
        return userRepository.findAll(pageable);
    }

    public List<User> getAllUsers() {
        logger.debug("Fetching all users");
        return userRepository.findAll();
    }

    public List<User> findByUniversityId(Long universityId) {
        logger.debug("Fetching users by university ID: {}", universityId);
        return userRepository.findByUniversityId(universityId);
    }

    public List<User> findByRole(UserRole role) {
        logger.debug("Fetching users by role: {}", role);
        return userRepository.findByRole(role);
    }

    @Transactional
    public User updateUser(Long userId, User userUpdates) {
        logger.info("Updating user with ID: {}", userId);

        User existingUser = getUserById(userId);

        // Update allowed fields
        if (userUpdates.getProfile() != null) {
            if (existingUser.getProfile() == null) {
                existingUser.setProfile(new Profile());
            }
            updateProfileFields(existingUser.getProfile(), userUpdates.getProfile());
        }

        existingUser.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(existingUser);
        logger.info("User updated successfully: {}", userId);

        return updatedUser;
    }

    @Transactional
    public void deleteUser(Long userId) {
        logger.info("Deleting user with ID: {}", userId);

        User user = getUserById(userId);
        userRepository.delete(user);

        logger.info("User deleted successfully: {}", userId);
    }

    @Transactional
    public User deactivateUser(Long userId) {
        logger.info("Deactivating user with ID: {}", userId);

        User user = getUserById(userId);
        user.setIsEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());

        User deactivatedUser = userRepository.save(user);
        logger.info("User deactivated: {}", userId);

        return deactivatedUser;
    }

    @Transactional
    public User activateUser(Long userId) {
        logger.info("Activating user with ID: {}", userId);

        User user = getUserById(userId);
        user.setIsEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());

        User activatedUser = userRepository.save(user);
        logger.info("User activated: {}", userId);

        return activatedUser;
    }

    @Transactional
    public User verifyEmail(Long userId) {
        logger.info("Verifying email for user ID: {}", userId);

        User user = getUserById(userId);
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());

        User verifiedUser = userRepository.save(user);
        logger.info("Email verified for user: {}", userId);

        return verifiedUser;
    }

    @Transactional
    public void updateLastLogin(Long userId) {
        logger.debug("Updating last login for user ID: {}", userId);

        User user = getUserById(userId);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public User changePassword(Long userId, String newPassword) {
        logger.info("Changing password for user ID: {}", userId);

        validationUtils.validateNotBlank(newPassword, "Password");
        validationUtils.validatePassword(newPassword);

        User user = getUserById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        logger.info("Password changed successfully for user: {}", userId);

        return updatedUser;
    }

    @Transactional
    public User updateUserRole(Long userId, UserRole newRole) {
        logger.info("Updating role for user ID: {} to: {}", userId, newRole);

        validationUtils.validateNotNull(newRole, "User role");

        User user = getUserById(userId);
        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        logger.info("User role updated: {} to {}", userId, newRole);

        return updatedUser;
    }

    public boolean isPasswordValid(Long userId, String rawPassword) {
        User user = getUserById(userId);
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    // ===== PRIVATE HELPER METHODS =====

    private void validateUserSignupRequest(UserSignupRequest request) {
        validationUtils.validateNotBlank(request.getEmail(), "Email");
        validationUtils.validateEmail(request.getEmail());
        validationUtils.validateNotBlank(request.getPassword(), "Password");
        validationUtils.validatePassword(request.getPassword());
        validationUtils.validateNotBlank(request.getFirstName(), "First name");
        validationUtils.validateNotBlank(request.getLastName(), "Last name");
        validationUtils.validateName(request.getFirstName(), "First name");
        validationUtils.validateName(request.getLastName(), "Last name");
        validationUtils.validateNotNull(request.getRole(), "User role");
    }

    private User createUserFromRequest(UserSignupRequest request) {
        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setIsEnabled(true);
        user.setEmailVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private Profile createProfileFromRequest(UserSignupRequest request) {
        Profile profile = new Profile();
        profile.setFirstName(validationUtils.sanitizeName(request.getFirstName()));
        profile.setLastName(validationUtils.sanitizeName(request.getLastName()));
        profile.setCreatedAt(LocalDateTime.now());
        return profile;
    }

    private void updateProfileFields(Profile existingProfile, Profile updatedProfile) {
        if (updatedProfile.getFirstName() != null) {
            existingProfile.setFirstName(validationUtils.sanitizeName(updatedProfile.getFirstName()));
        }
        if (updatedProfile.getLastName() != null) {
            existingProfile.setLastName(validationUtils.sanitizeName(updatedProfile.getLastName()));
        }
        if (updatedProfile.getBio() != null) {
            existingProfile.setBio(validationUtils.sanitizeInput(updatedProfile.getBio()));
        }
        if (updatedProfile.getProfilePictureUrl() != null) {
            existingProfile.setProfilePictureUrl(updatedProfile.getProfilePictureUrl());
        }
        existingProfile.setUpdatedAt(LocalDateTime.now());
    }

    private void sendVerificationEmailAsync(User user) {
        CompletableFuture<Boolean> emailFuture = emailService.sendVerificationEmail(user);

        emailFuture.thenAccept(success -> {
            if (success) {
                logger.info("Verification email sent successfully to: {}", user.getEmail());
            } else {
                logger.warn("Failed to send verification email to: {}", user.getEmail());
                // In production, you might want to retry or log to monitoring system
            }
        }).exceptionally(throwable -> {
            logger.error("Exception occurred while sending verification email to: {}", user.getEmail(), throwable);
            return null;
        });
    }

    // ===== BUSINESS LOGIC METHODS =====

    public boolean canUserTakeTest(Long userId) {
        User user = getUserById(userId);
        return user.getIsEnabled() && user.getEmailVerified();
    }

    public boolean isUserUniversityAdmin(Long userId, Long universityId) {
        User user = getUserById(userId);
        return user.getUniversity() != null &&
                user.getUniversity().getId().equals(universityId) &&
                user.getRole() == UserRole.UNIVERSITY_ADMIN;
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getActiveUserCount() {
        return userRepository.countByIsEnabledTrue();
    }

    public long getVerifiedUserCount() {
        return userRepository.countByEmailVerifiedTrue();
    }
}