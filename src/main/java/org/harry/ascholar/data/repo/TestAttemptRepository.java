//package org.harry.ascholar.data.repo;
//
//import org.harry.ascholar.data.enums.TestStatus;
//import org.harry.ascholar.data.models.TestAttempt;
//import org.harry.ascholar.data.models.User;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
//    List<TestAttempt> findByUserId(Long userId);
//    List<TestAttempt> findByTestId(Long testId);
//    Page<TestAttempt> findByTestId(Long testId, Pageable pageable);
//    List<TestAttempt> findByStatus(TestStatus status);
//    List<TestAttempt> findByStatusIn(List<TestStatus> statuses);
//    Optional<TestAttempt> findByTestIdAndUserIdAndStatusIn(Long testId, Long userId, List<TestStatus> statuses);
//    long countByStatus(TestStatus status);
//}


package org.harry.ascholar.data.repo;

import org.harry.ascholar.data.models.TestAttempt;
import org.harry.ascholar.data.enums.TestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ta FROM TestAttempt ta WHERE ta.test.id = :testId AND ta.user.id = :userId AND ta.status IN :statuses")
    Optional<TestAttempt> findByTestIdAndUserIdAndStatusInWithLock(
            @Param("testId") Long testId,
            @Param("userId") Long userId,
            @Param("statuses") List<TestStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT ta FROM TestAttempt ta WHERE ta.status = :status")
    List<TestAttempt> findByStatusWithLock(@Param("status") TestStatus status);

    @Query("SELECT ta FROM TestAttempt ta JOIN FETCH ta.user WHERE ta.id = :id")
    Optional<TestAttempt> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT ta FROM TestAttempt ta JOIN FETCH ta.user WHERE ta.user.id = :userId")
    List<TestAttempt> findByUserIdWithUser(@Param("userId") Long userId);

    Optional<TestAttempt> findByTestIdAndUserIdAndStatusIn(Long testId, Long userId, List<TestStatus> registered);

    List<TestAttempt> findByStatus(TestStatus testStatus);

    List<TestAttempt> findByUserId(Long userId);

    Page<TestAttempt> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Add indexes in database for these queries
}