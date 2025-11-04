package org.harry.ascholar.data.repo;


import org.harry.ascholar.data.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
     // === BASIC CRUD OPERATIONS ===

    /**
     * Find audit log by ID with all relations fetched
     * Index: CREATE INDEX idx_audit_log_id ON audit_logs(id);
     */
    @Query("SELECT al FROM AuditLog al WHERE al.id = :id")
    Optional<AuditLog> findById(@Param("id") Long id);

    // === SEARCH & FILTER OPERATIONS ===

    /**
     * Find audit logs by user ID with pagination
     * Index: CREATE INDEX idx_audit_log_user_id ON audit_logs(user_id);
     */
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId ORDER BY al.timestamp DESC")
    Page<AuditLog> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find audit logs by action type with pagination
     * Index: CREATE INDEX idx_audit_log_action ON audit_logs(action);
     */
    @Query("SELECT al FROM AuditLog al WHERE al.action = :action ORDER BY al.timestamp DESC")
    Page<AuditLog> findByAction(@Param("action") String action, Pageable pageable);

    /**
     * Find audit logs by user ID and action type
     */
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId AND al.action = :action ORDER BY al.timestamp DESC")
    Page<AuditLog> findByUserIdAndAction(@Param("userId") Long userId,
                                         @Param("action") String action,
                                         Pageable pageable);

    /**
     * Find audit logs within a time range
     * Index: CREATE INDEX idx_audit_log_timestamp ON audit_logs(timestamp);
     */
    @Query("SELECT al FROM AuditLog al WHERE al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    Page<AuditLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

    /**
     * Find audit logs by user ID within a time range
     */
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId AND al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    Page<AuditLog> findByUserIdAndTimestampBetween(@Param("userId") Long userId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   Pageable pageable);

    /**
     * Find audit logs by IP address
     * Index: CREATE INDEX idx_audit_log_ip ON audit_logs(ip_address);
     */
    @Query("SELECT al FROM AuditLog al WHERE al.ipAddress = :ipAddress ORDER BY al.timestamp DESC")
    Page<AuditLog> findByIpAddress(@Param("ipAddress") String ipAddress, Pageable pageable);

    /**
     * Advanced search with multiple optional criteria
     */
    @Query("SELECT al FROM AuditLog al WHERE " +
            "(:userId IS NULL OR al.userId = :userId) AND " +
            "(:action IS NULL OR al.action = :action) AND " +
            "(:ipAddress IS NULL OR al.ipAddress = :ipAddress) AND " +
            "(:startDate IS NULL OR al.timestamp >= :startDate) AND " +
            "(:endDate IS NULL OR al.timestamp <= :endDate) " +
            "ORDER BY al.timestamp DESC")
    Page<AuditLog> searchAuditLogs(@Param("userId") Long userId,
                                   @Param("action") String action,
                                   @Param("ipAddress") String ipAddress,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   Pageable pageable);

    // === ANALYTICS & REPORTING ===

    /**
     * Count audit logs by action type
     */
    @Query("SELECT al.action, COUNT(al) FROM AuditLog al GROUP BY al.action ORDER BY COUNT(al) DESC")
    List<Object[]> countByAction();

    /**
     * Count audit logs by user ID
     */
    @Query("SELECT al.userId, COUNT(al) FROM AuditLog al WHERE al.userId IS NOT NULL GROUP BY al.userId ORDER BY COUNT(al) DESC")
    List<Object[]> countByUserId();

    /**
     * Get daily audit log counts for the last N days
     */
    @Query(value =
            "SELECT DATE(timestamp) as audit_date, COUNT(*) as count " +
                    "FROM audit_logs " +
                    "WHERE timestamp >= :startDate " +
                    "GROUP BY DATE(timestamp) " +
                    "ORDER BY audit_date DESC",
            nativeQuery = true)
    List<Object[]> getDailyAuditCounts(@Param("startDate") LocalDateTime startDate);

    /**
     * Get top users by audit log count
     */
    @Query("SELECT al.userId, COUNT(al) as count FROM AuditLog al WHERE al.userId IS NOT NULL GROUP BY al.userId ORDER BY count DESC")
    Page<Object[]> findTopUsersByAuditCount(Pageable pageable);
        /*
          Get audit log statistics by hour of day
        */
        @Query(value =
                "SELECT EXTRACT(HOUR FROM timestamp) as hour, COUNT(*) as count " +
                        "FROM audit_logs " +
                        "GROUP BY EXTRACT(HOUR FROM timestamp) " +
                        "ORDER BY hour",
                nativeQuery = true)
        List<Object[]> getAuditCountsByHour();

        // === MAINTENANCE & CLEANUP OPERATIONS ===

    /**
     * Delete audit logs older than specified date (for data retention policies)
     */
    @Modifying
    @Query("DELETE FROM AuditLog al WHERE al.timestamp < :cutoffDate")
    int deleteByTimestampBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete audit logs for a specific user (GDPR compliance)
     */
    @Modifying @Query("DELETE FROM AuditLog al WHERE al.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * Find audit logs that need to be archived (older than retention period)
     */
    @Query("SELECT al FROM AuditLog al WHERE al.timestamp < :cutoffDate ORDER BY al.timestamp ASC")
    Page<AuditLog> findAuditLogsForArchiving(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    /**
     * Count total audit logs (for monitoring)
     */
    @Query("SELECT COUNT(al) FROM AuditLog al")
    long countTotalAuditLogs();

    /**
     * Count audit logs before a certain date (for cleanup planning)
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.timestamp < :cutoffDate")
    long countByTimestampBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    // === SECURITY & COMPLIANCE ===

    /**
     * Find failed login attempts for a user within time window
     */
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId AND al.action = 'LOGIN_FAILED' AND al.timestamp >= :since ORDER BY al.timestamp DESC")
    List<AuditLog> findRecentFailedLogins(@Param("userId") Long userId,
                                          @Param("since") LocalDateTime since);

    /**
     * Count failed login attempts by IP address (for brute force detection)
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.ipAddress = :ipAddress AND al.action = 'LOGIN_FAILED' AND al.timestamp >= :since")
    long countFailedLoginsByIpAddress(@Param("ipAddress") String ipAddress,
                                      @Param("since") LocalDateTime since);

    /**
     * Find suspicious activities (multiple failed logins, etc.)
     */
    @Query("SELECT al.userId, al.ipAddress, COUNT(al) as attemptCount " +
            "FROM AuditLog al " +
            "WHERE al.action = 'LOGIN_FAILED' AND al.timestamp >= :since " +
            "GROUP BY al.userId, al.ipAddress " +
            "HAVING COUNT(al) >= :threshold")
    List<Object[]> findSuspiciousActivities(@Param("since") LocalDateTime since,
                                            @Param("threshold") long threshold);

    // === PERFORMANCE OPTIMIZATIONS ===

    /**
         * Check if audit log exists for user and action (for duplicate detection)
         */
        @Query("SELECT CASE WHEN COUNT(al) > 0 THEN true ELSE false END FROM AuditLog al WHERE al.userId = :userId AND al.action = :action AND al.timestamp >= :since")
        boolean existsByUserIdAndActionSince(@Param("userId") Long userId,
                                             @Param("action") String action,
                                             @Param("since") LocalDateTime since);

        /**
         * Get latest audit log for a user
         */
        @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId ORDER BY al.timestamp DESC")
        Page<AuditLog> findLatestByUserId(@Param("userId") Long userId, Pageable pageable);

        /**
         * Get audit logs with specific metadata key-value pair
         */
        @Query(value =
                "SELECT al.* FROM audit_logs al " +
                        "WHERE al.metadata @> :metadataJson::jsonb",
                nativeQuery = true)
        Page<AuditLog> findByMetadataContaining(@Param("metadataJson") String metadataJson, Pageable pageable);

        // === BATCH OPERATIONS ===

        /**
         * Find audit logs in batches for processing
         */
        @Query("SELECT al FROM AuditLog al WHERE al.id > :lastId ORDER BY al.id ASC")
        List<AuditLog> findBatchAfterId(@Param("lastId") Long lastId, Pageable pageable);

        /**
         * Count audit logs by date range (for reporting)
         */
        @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.timestamp BETWEEN :startDate AND :endDate") long countByTimestampBetween(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);
    }

