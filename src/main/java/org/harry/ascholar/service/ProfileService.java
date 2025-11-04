package org.harry.ascholar.service;

import org.harry.ascholar.data.models.Profile;
import org.harry.ascholar.data.models.Education;
import org.harry.ascholar.data.repo.ProfileRepository;
import org.harry.ascholar.exceptions.ResourceNotFoundException;
import org.harry.ascholar.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProfileService {
    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private final ProfileRepository profileRepository;
    private final ValidationUtils validationUtils;

    @Autowired
    public ProfileService(ProfileRepository profileRepository, ValidationUtils validationUtils) {
        this.profileRepository = profileRepository;
        this.validationUtils = validationUtils;
    }

    public Profile getProfileById(Long id) {
        logger.debug("Fetching profile by ID: {}", id);
        return profileRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Profile not found with ID: {}", id);
                    return new ResourceNotFoundException("Profile", "id", id);
                });
    }

    public Optional<Profile> getProfileByUserId(Long userId) {
        logger.debug("Fetching profile by user ID: {}", userId);
        return profileRepository.findByUserId(userId);
    }

    @Transactional
    public Profile updateProfile(Long profileId, Profile profileUpdates) {
        logger.info("Updating profile with ID: {}", profileId);

        Profile existingProfile = getProfileById(profileId);

            // Update allowed fields with validation
        if (profileUpdates.getFirstName() != null) {
            validationUtils.validateName(profileUpdates.getFirstName(), "First name");
            existingProfile.setFirstName(validationUtils.sanitizeName(profileUpdates.getFirstName()));
        }

        if (profileUpdates.getLastName() != null) {
            validationUtils.validateName(profileUpdates.getLastName(), "Last name");
            existingProfile.setLastName(validationUtils.sanitizeName(profileUpdates.getLastName()));
        }

        if (profileUpdates.getBio() != null) {
            existingProfile.setBio(validationUtils.sanitizeInput(profileUpdates.getBio()));
        }

        if (profileUpdates.getProfilePictureUrl() != null) {
            validationUtils.validateUrl(profileUpdates.getProfilePictureUrl(), "Profile picture URL");
            existingProfile.setProfilePictureUrl(profileUpdates.getProfilePictureUrl());
        }

        Profile updatedProfile = profileRepository.save(existingProfile);
        logger.info("Profile updated successfully: {}", profileId);

        return updatedProfile;
    }

    @Transactional
    public Profile addEducation(Long profileId, Education education) {
        logger.info("Adding education to profile: {}", profileId);

        validationUtils.validateNotBlank(education.getInstitution(), "Institution");
        validationUtils.validateNotBlank(education.getDegree(), "Degree");

        Profile profile = getProfileById(profileId);
        profile.getEducationHistory().add(education);

        Profile updatedProfile = profileRepository.save(profile);
        logger.info("Education added to profile: {}", profileId);

        return updatedProfile;
    }

    @Transactional
    public Profile updateEducation(Long profileId, int educationIndex, Education education) {
        logger.info("Updating education at index {} for profile: {}", educationIndex, profileId);

        Profile profile = getProfileById(profileId);

        if (educationIndex < 0 || educationIndex >= profile.getEducationHistory().size()) {
            throw new IllegalArgumentException("Invalid education index: " + educationIndex);
        }

        profile.getEducationHistory().set(educationIndex, education);
        Profile updatedProfile = profileRepository.save(profile);
        logger.info("Education updated for profile: {}", profileId);

        return updatedProfile;
    }

    @Transactional
    public Profile removeEducation(Long profileId, int educationIndex) {
        logger.info("Removing education at index {} from profile: {}", educationIndex, profileId);

        Profile profile = getProfileById(profileId);

        if (educationIndex < 0 || educationIndex >= profile.getEducationHistory().size()) {
            throw new IllegalArgumentException("Invalid education index: " + educationIndex);
        }

        profile.getEducationHistory().remove(educationIndex);
        Profile updatedProfile = profileRepository.save(profile);
        logger.info("Education removed from profile: {}", profileId);

        return updatedProfile;
    }

    @Transactional
    public Profile addSkill(Long profileId, String skill) {
        logger.info("Adding skill to profile: {}", profileId);

        validationUtils.validateNotBlank(skill, "Skill");
        String sanitizedSkill = validationUtils.sanitizeInput(skill.trim());

        Profile profile = getProfileById(profileId);

        if (!profile.getSkills().contains(sanitizedSkill)) {
            profile.getSkills().add(sanitizedSkill);
        }

        Profile updatedProfile = profileRepository.save(profile);
        logger.info("Skill '{}' added to profile: {}", sanitizedSkill, profileId);

        return updatedProfile;
    }

    @Transactional
    public Profile removeSkill(Long profileId, String skill) {
        logger.info("Removing skill from profile: {}", profileId);

        validationUtils.validateNotBlank(skill, "Skill");
        String sanitizedSkill = validationUtils.sanitizeInput(skill.trim());

        Profile profile = getProfileById(profileId);
        profile.getSkills().remove(sanitizedSkill);

        Profile updatedProfile = profileRepository.save(profile);
        logger.info("Skill '{}' removed from profile: {}", sanitizedSkill, profileId);

        return updatedProfile;
    }

    @Transactional
    public Profile updateProfilePicture(Long profileId, String profilePictureUrl) {
        logger.info("Updating profile picture for profile: {}", profileId);

        validationUtils.validateUrl(profilePictureUrl, "Profile picture URL");

        Profile profile = getProfileById(profileId);
        profile.setProfilePictureUrl(profilePictureUrl);

        Profile updatedProfile = profileRepository.save(profile);
        logger.info("Profile picture updated for profile: {}", profileId);

        return updatedProfile;
    }

    @Transactional
    public void deleteProfile(Long profileId) {
        logger.info("Deleting profile: {}", profileId);

        Profile profile = getProfileById(profileId);
        profileRepository.delete(profile);

        logger.info("Profile deleted: {}", profileId);
    }

    public List<Profile> searchProfilesBySkill(String skill) {
        logger.debug("Searching profiles by skill: {}", skill);
        validationUtils.validateNotBlank(skill, "Skill");

        return profileRepository.findBySkillsContaining(skill.toLowerCase());
    }

    public List<Profile> searchProfilesByName(String name) {
        logger.debug("Searching profiles by name: {}", name);
        validationUtils.validateNotBlank(name, "Name");

        return profileRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(name, name);
    }

    public long getTotalProfileCount() {
        return profileRepository.count();
    }
}
