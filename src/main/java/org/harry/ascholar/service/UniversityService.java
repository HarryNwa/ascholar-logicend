package org.harry.ascholar.service;


import org.harry.ascholar.data.models.University;
import org.harry.ascholar.data.models.User;
import org.harry.ascholar.data.repo.UniversityRepository;
import org.harry.ascholar.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UniversityService {
    private static final Logger logger = LoggerFactory.getLogger(UniversityService.class);

    private final org.harry.ascholar.data.repo.UniversityRepository universityRepository;
    private final UserService userService;

    @Autowired
    public UniversityService(UniversityRepository universityRepository, UserService userService) {
        this.universityRepository = universityRepository;
        this.userService = userService;
    }

    public List<University> getAllUniversities() {
        return universityRepository.findAll();
    }

    public Page<University> getAllUniversities(Pageable pageable) {
        return universityRepository.findAll(pageable);
    }

    public List<University> getVerifiedUniversities() {
        return universityRepository.findByIsVerifiedTrue();
    }

    public University getUniversityById(Long id) {
        return universityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("University", "id", id));
    }

    public List<University> searchUniversities(String name) {
        return universityRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public University createUniversity(String name, String description) {
        University university = new University();
        university.setName(name.trim());
        university.setDescription(description);
        university.setIsVerified(false);
        university.setCreatedAt(LocalDateTime.now());
        university.setUpdatedAt(LocalDateTime.now());

        University savedUniversity = universityRepository.save(university);
        logger.info("University created: {}", name);
        return savedUniversity;
    }

    @Transactional
    public University updateUniversity(Long id, String name, String description) {
        University university = getUniversityById(id);
        university.setName(name.trim());
        university.setDescription(description);
        university.setUpdatedAt(LocalDateTime.now());

        University updatedUniversity = universityRepository.save(university);
        logger.info("University updated: {}", name);
        return updatedUniversity;
    }

    @Transactional
    public University verifyUniversity(Long id) {
        University university = getUniversityById(id);
        university.setIsVerified(true);
        university.setUpdatedAt(LocalDateTime.now());

        University verifiedUniversity = universityRepository.save(university);
        logger.info("University verified: {}", university.getName());
        return verifiedUniversity;
    }

    @Transactional
    public University unverifyUniversity(Long id) {
        University university = getUniversityById(id);
        university.setIsVerified(false);
        university.setUpdatedAt(LocalDateTime.now());

        University unverifiedUniversity = universityRepository.save(university);
        logger.info("University unverified: {}", university.getName());
        return unverifiedUniversity;
    }

    @Transactional
    public void deleteUniversity(Long id) {
        University university = getUniversityById(id);
        universityRepository.delete(university);
        logger.info("University deleted: {}", id);
    }

    @Transactional
    public void assignUserToUniversity(Long userId, Long universityId) {
        User user = userService.getUserById(userId);
        University university = getUniversityById(universityId);

        user.setUniversity(university);
        user.setUpdatedAt(LocalDateTime.now());
        userService.save(user);

        logger.info("User {} assigned to university {}", userId, universityId);
    }

    @Transactional
    public void removeUserFromUniversity(Long userId) {
        User user = userService.getUserById(userId);
        user.setUniversity(null);
        user.setUpdatedAt(LocalDateTime.now());
        userService.save(user);

        logger.info("User {} removed from university", userId);
    }

    public List<User> getUniversityAdmins(Long universityId) {
        University university = getUniversityById(universityId);
        // This would typically involve a custom query
        return userService.findByUniversityId(universityId);
    }

    @Transactional
    public University updateUniversityLogo(Long universityId, String logoUrl) {
        University university = getUniversityById(universityId);
        university.setLogoUrl(logoUrl);
        university.setUpdatedAt(LocalDateTime.now());

        University updatedUniversity = universityRepository.save(university);
        logger.info("University logo updated: {}", universityId);
        return updatedUniversity;
    }
}

