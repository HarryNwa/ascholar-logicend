package org.harry.ascholar.data.repo;

import org.harry.ascholar.data.models.Notification;
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
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // === DERIVED QUERY METHODS (Spring Data JPA auto-implements these) ===

    /**
     * Find notifications by user ID with pagination and sorting
     * Spring Data JPA automatically creates: SELECT n FROM Notification n WHERE n.user.id = ?1 ORDER BY n.createdAt DESC
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find unread notifications by user ID
     * Spring Data JPA automatically creates: SELECT n FROM Notification n WHERE n.user.id = ?1 AND n.read = false ORDER BY n.createdAt DESC
     */
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Count unread notifications by user ID
     * Spring Data JPA automatically creates: SELECT COUNT(n) FROM Notification n WHERE n.user.id = ?1 AND n.read = false
     */
    long countByUserIdAndReadFalse(@Param("userId") Long userId);

    /**
     * Delete read notifications older than cutoff date
     * Spring Data JPA automatically creates: DELETE FROM Notification n WHERE n.read = true AND n.createdAt < ?1
     */
    long deleteByReadTrueAndCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    // === CUSTOM QUERY METHODS (Complex queries that need optimization) ===

    @Query("SELECT n FROM Notification n JOIN FETCH n.user WHERE n.id = :id")
    Optional<Notification> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.read = false ORDER BY n.priority DESC, n.createdAt DESC")
    Page<Notification> findUnreadByUserIdWithPaging(@Param("userId") Long userId, Pageable pageable);

    // === SEARCH AND FILTER OPERATIONS ===

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndType(@Param("userId") Long userId, @Param("type") String type, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.category = :category ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndCategory(@Param("userId") Long userId, @Param("category") String category, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.priority = :priority ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndPriority(@Param("userId") Long userId, @Param("priority") Notification.NotificationPriority priority, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);

    // === COUNT OPERATIONS ===

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.type = :type AND n.read = false")
    long countUnreadByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    // === BULK OPERATIONS ===

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.read = false")
    int markAllAsReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.id IN :ids AND n.user.id = :userId")
    int markAsReadByIdsAndUserId(@Param("ids") List<Long> ids, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND n.read = true AND n.createdAt < :cutoffDate")
    int deleteReadByUserIdAndCreatedAtBefore(@Param("userId") Long userId, @Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt < :currentDate")
    int deleteExpiredNotifications(@Param("currentDate") LocalDateTime currentDate);

    // === ANALYTICS AND REPORTING ===

    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.user.id = :userId GROUP BY n.type ORDER BY COUNT(n) DESC")
    List<Object[]> countByTypeForUser(@Param("userId") Long userId);

    @Query("SELECT n.category, COUNT(n) FROM Notification n WHERE n.user.id = :userId GROUP BY n.category ORDER BY COUNT(n) DESC")
    List<Object[]> countByCategoryForUser(@Param("userId") Long userId);

    @Query("SELECT n.priority, COUNT(n) FROM Notification n WHERE n.user.id = :userId GROUP BY n.priority ORDER BY n.priority DESC")
    List<Object[]> countByPriorityForUser(@Param("userId") Long userId);

    @Query(value =
            "SELECT DATE(created_at) as notification_date, COUNT(*) as count " +
                    "FROM notifications " +
                    "WHERE user_id = :userId AND created_at >= :startDate " +
                    "GROUP BY DATE(created_at) " +
                    "ORDER BY notification_date DESC",
            nativeQuery = true)
    List<Object[]> getDailyNotificationCounts(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    // === EXPIRATION AND MAINTENANCE ===

    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :currentDate")
    List<Notification> findExpiredNotifications(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.expiresAt IS NOT NULL AND n.expiresAt < :currentDate")
    List<Notification> findExpiredNotificationsForUser(@Param("userId") Long userId, @Param("currentDate") LocalDateTime currentDate);

    // === SECURITY AND VALIDATION ===

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Notification n WHERE n.id = :id AND n.user.id = :userId")
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Optional<Notification> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    // === BATCH FETCHING FOR PERFORMANCE ===

    @Query("SELECT n FROM Notification n WHERE n.user.id IN :userIds AND n.read = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT n FROM Notification n WHERE n.sourceType = :sourceType AND n.sourceId = :sourceId")
    List<Notification> findBySource(@Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);

    // === RECENT NOTIFICATIONS ===

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.read = false ORDER BY n.priority DESC, n.createdAt DESC")
    List<Notification> findRecentUnreadByUserId(@Param("userId") Long userId, Pageable pageable);
}