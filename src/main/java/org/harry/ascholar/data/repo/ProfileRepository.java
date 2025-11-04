package org.harry.ascholar.data.repo;

import org.harry.ascholar.data.models.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    // === CORE PROFILE OPERATIONS ===

    /**
     * Find profile by user ID with user entity fetched (prevents N+1 queries)
     * Index: CREATE INDEX idx_profile_user_id ON profiles(user_id);
     */
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE p.user.id = :userId")
    Optional<Profile> findByUserId(@Param("userId") Long userId);

    /**
     * Find profile by user ID with pessimistic lock for updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Profile p WHERE p.user.id = :userId")
    Optional<Profile> findByUserIdWithLock(@Param("userId") Long userId);

    /**
     * Check if profile exists for user
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Profile p WHERE p.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);

    // === TALENT DISCOVERY & FILTERING ===

    /**
     * Find visible profiles by score range with pagination
     * Index: CREATE INDEX idx_profile_score_visible ON profiles(overall_score) WHERE is_visible_to_universities = true;
     */
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE p.overallScore BETWEEN :minScore AND :maxScore AND p.isVisibleToUniversities = true")
    Page<Profile> findByScoreRange(@Param("minScore") BigDecimal minScore,
                                   @Param("maxScore") BigDecimal maxScore,
                                   Pageable pageable);

    /**
     * Find top performers above minimum score
     */
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE p.overallScore >= :minScore AND p.isVisibleToUniversities = true ORDER BY p.overallScore DESC")
    Page<Profile> findByOverallScoreGreaterThanEqual(@Param("minScore") BigDecimal minScore, Pageable pageable);

    /**
     * Search by desired program (case-insensitive, partial match)
     * Index: CREATE INDEX idx_profile_program ON profiles(desired_program);
     */
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE LOWER(p.desiredProgram) LIKE LOWER(CONCAT('%', :program, '%')) AND p.isVisibleToUniversities = true")
    Page<Profile> findByDesiredProgramContainingIgnoreCase(@Param("program") String program, Pageable pageable);

    /**
     * Search by location (case-insensitive, partial match)
     * Index: CREATE INDEX idx_profile_location ON profiles(location);
     */
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE LOWER(p.location) LIKE LOWER(CONCAT('%', :location, '%')) AND p.isVisibleToUniversities = true")
    Page<Profile> findByLocationContainingIgnoreCase(@Param("location") String location, Pageable pageable);

    /**
     * Search by skill (exact match in skills collection)
     */
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE :skill MEMBER OF p.skills AND p.isVisibleToUniversities = true")
    Page<Profile> findBySkill(@Param("skill") String skill, Pageable pageable);

    /**
     * Search by multiple skills (at least one match)
     */
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE p.isVisibleToUniversities = true AND EXISTS (SELECT s FROM p.skills s WHERE s IN :skills)")
    Page<Profile> findByAnySkill(@Param("skills") Set<String> skills, Pageable pageable);

    /**
     * Advanced search with multiple optional criteria
     */
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE " +
            "(:program IS NULL OR LOWER(p.desiredProgram) LIKE LOWER(CONCAT('%', :program, '%'))) AND " +
            "(:location IS NULL OR LOWER(p.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:minScore IS NULL OR p.overallScore >= :minScore) AND " +
            "(:maxScore IS NULL OR p.overallScore <= :maxScore) AND " +
            "(:skills IS NULL OR EXISTS (SELECT s FROM p.skills s WHERE s IN :skills)) AND " +
            "p.isVisibleToUniversities = true")
    Page<Profile> searchProfiles(@Param("program") String program,
                                 @Param("location") String location,
                                 @Param("minScore") BigDecimal minScore,
                                 @Param("maxScore") BigDecimal maxScore,
                                 @Param("skills") Set<String> skills,
                                 Pageable pageable);

    /**
     * Get all visible profiles ordered by score (for talent dashboard)
     */
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE p.isVisibleToUniversities = true ORDER BY p.overallScore DESC NULLS LAST, p.updatedAt DESC")
    Page<Profile> findVisibleProfilesOrderByScore(Pageable pageable);

    // === BATCH OPERATIONS ===

    /**
     * Find multiple profiles by user IDs (for batch processing)
     */
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE p.user.id IN :userIds")
    List<Profile> findByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * Find profiles that need completion reminders
     */
    @Query("SELECT p FROM Profile p WHERE p.profileCompletionPercentage < :threshold AND p.updatedAt < :cutoffDate")
    List<Profile> findIncompleteProfiles(@Param("threshold") Integer threshold,
                                         @Param("cutoffDate") LocalDateTime cutoffDate);

    // === ANALYTICS & REPORTING ===

    /**
     * Calculate average score across all profiles
     */
    @Query("SELECT AVG(p.overallScore) FROM Profile p WHERE p.overallScore IS NOT NULL")
    Optional<BigDecimal> findAverageOverallScore();

    /**
     * Count profiles by completion percentage threshold
     */
    @Query("SELECT COUNT(p) FROM Profile p WHERE p.profileCompletionPercentage >= :completionThreshold")
    Long countByProfileCompletionPercentageGreaterThanEqual(@Param("completionThreshold") Integer completionThreshold);

    /**
     * Get program statistics (program name, count, average score)
     */
    @Query("SELECT p.desiredProgram, COUNT(p), AVG(p.overallScore) FROM Profile p WHERE p.overallScore IS NOT NULL AND p.desiredProgram IS NOT NULL GROUP BY p.desiredProgram")
    List<Object[]> findProgramStatistics();

    /**
     * Get location distribution
     */
    @Query("SELECT p.location, COUNT(p) FROM Profile p WHERE p.location IS NOT NULL GROUP BY p.location")
    List<Object[]> findLocationDistribution();

    /**
     * Get skill popularity statistics
     */
    @Query(value =
            "SELECT skill, COUNT(*) as count FROM (" +
                    "  SELECT unnest(CAST(p.skills AS text[])) as skill FROM profiles p" +
                    ") skills GROUP BY skill ORDER BY count DESC LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findPopularSkills(@Param("limit") int limit);

    /**
     * Get visibility statistics
     */
    @Query("SELECT p.isVisibleToUniversities, COUNT(p) FROM Profile p GROUP BY p.isVisibleToUniversities")
    List<Object[]> findVisibilityStatistics();

    // === PERFORMANCE & MAINTENANCE ===

    /**
     * Count total visible profiles (for dashboard metrics)
     */
    @Query("SELECT COUNT(p) FROM Profile p WHERE p.isVisibleToUniversities = true")
    long countVisibleProfiles();

    /**
     * Find profiles updated before a certain date (for cleanup)
     */
    @Query("SELECT p FROM Profile p WHERE p.updatedAt < :cutoffDate")
    Page<Profile> findProfilesUpdatedBefore(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    /**
     * Get profiles with high scores for notification (top 10%)
     */
    @Query(value =
            "SELECT p.* FROM profiles p " +
                    "WHERE p.overall_score >= (SELECT PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY overall_score) FROM profiles) " +
                    "AND p.is_visible_to_universities = true",
            nativeQuery = true)
    List<Profile> findTopPercentileProfiles();

    // === ADDITIONAL UTILITY METHODS ===

    /**
     * Check if a user has a completed profile
     */
    @Query("SELECT CASE WHEN p.profileCompletionPercentage >= 80 THEN true ELSE false END FROM Profile p WHERE p.user.id = :userId")
    Optional<Boolean> isProfileCompleted(@Param("userId") Long userId);

    /**
     * Find profiles by degree level
     */
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE p.degreeLevel = :degreeLevel AND p.isVisibleToUniversities = true")
    Page<Profile> findByDegreeLevel(@Param("degreeLevel") String degreeLevel, Pageable pageable);

    /**
     * Get profile completion statistics
     */
    @Query("SELECT " +
            "SUM(CASE WHEN p.profileCompletionPercentage >= 90 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN p.profileCompletionPercentage >= 70 AND p.profileCompletionPercentage < 90 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN p.profileCompletionPercentage < 70 THEN 1 ELSE 0 END) " +
            "FROM Profile p")
    Object[] getCompletionStatistics();

    // === DERIVED QUERY METHODS ===

    /**
     * Find profiles containing specific skill (case-sensitive)
     */
    List<Profile> findBySkillsContaining(String skill);

    /**
     * Find profiles by first or last name (case-insensitive)
     */
    List<Profile> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    /**
     * Find visible profiles with pagination
     */
    Page<Profile> findByIsVisibleToUniversitiesTrue(Pageable pageable);

    /**
     * Find profiles by desired program
     */
    List<Profile> findByDesiredProgramContainingIgnoreCase(String program);

    /**
     * Find profiles by overall score range
     */
    Page<Profile> findByOverallScoreBetween(BigDecimal minScore, BigDecimal maxScore, Pageable pageable);

    /**
     * Find profiles by degree level
     */
    List<Profile> findByDegreeLevel(String degreeLevel);

    /**
     * Count active profiles
     */
    long countByIsVisibleToUniversitiesTrue();

    // === PERFORMANCE OPTIMIZATION QUERIES ===

    /**
     * Find profiles with test scores for batch processing
     */
    @Query("SELECT p FROM Profile p LEFT JOIN FETCH p.testScores WHERE p.user.id IN :userIds")
    List<Profile> findByUserIdsWithTestScores(@Param("userIds") List<Long> userIds);

    /**
     * Find recently updated profiles for cache refresh
     */
    @Query("SELECT p FROM Profile p WHERE p.updatedAt > :sinceDate ORDER BY p.updatedAt DESC")
    List<Profile> findRecentlyUpdatedProfiles(@Param("sinceDate") LocalDateTime sinceDate, Pageable pageable);

    /**
     * Get profile statistics for dashboard
     */
    @Query("SELECT " +
            "COUNT(p), " +
            "AVG(p.overallScore), " +
            "MAX(p.overallScore), " +
            "MIN(p.overallScore), " +
            "AVG(p.profileCompletionPercentage) " +
            "FROM Profile p WHERE p.isVisibleToUniversities = true")
    Object[] getDashboardStatistics();

    // === BULK OPERATIONS ===

    /**
     * Update visibility for multiple profiles
     */
    @Query("UPDATE Profile p SET p.isVisibleToUniversities = :isVisible WHERE p.user.id IN :userIds")
    void updateVisibilityForUsers(@Param("userIds") List<Long> userIds, @Param("isVisible") Boolean isVisible);

    /**
     * Bulk update last test taken timestamp
     */
    @Query("UPDATE Profile p SET p.lastTestTaken = :timestamp WHERE p.user.id IN :userIds")
    void updateLastTestTaken(@Param("userIds") List<Long> userIds, @Param("timestamp") LocalDateTime timestamp);
}