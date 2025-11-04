package org.harry.ascholar.data.repo;

import org.harry.ascholar.data.models.TestAnswer;
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
    public interface TestAnswerRepository extends JpaRepository<TestAnswer, Long> {

        @Query("SELECT ta FROM TestAnswer ta WHERE ta.attempt.id = :attemptId ORDER BY ta.answeredAt ASC")
        List<TestAnswer> findByAttemptId(@Param("attemptId") Long attemptId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT ta FROM TestAnswer ta WHERE ta.attempt.id = :attemptId AND ta.questionId = :questionId")
        Optional<TestAnswer> findByAttemptIdAndQuestionId(@Param("attemptId") Long attemptId,
                                                          @Param("questionId") Long questionId);

        @Query("SELECT COUNT(ta) FROM TestAnswer ta WHERE ta.attempt.id = :attemptId AND ta.isCorrect = true")
        Long countCorrectAnswersByAttemptId(@Param("attemptId") Long attemptId);

        @Query("SELECT COUNT(ta) FROM TestAnswer ta WHERE ta.attempt.id = :attemptId AND ta.answer IS NOT NULL")
        Long countAnsweredQuestionsByAttemptId(@Param("attemptId") Long attemptId);

        @Query("SELECT ta FROM TestAnswer ta WHERE ta.attempt.id IN :attemptIds")
        List<TestAnswer> findByAttemptIds(@Param("attemptIds") List<Long> attemptIds);

        @Query(value = "SELECT ta.question_id, COUNT(ta) FROM test_answers ta " +
                "WHERE ta.attempt_id IN (SELECT id FROM test_attempts WHERE test_id = :testId) " +
                "AND ta.is_correct = true GROUP BY ta.question_id",
                nativeQuery = true)
        List<Object[]> findCorrectAnswerCountsByTestId(@Param("testId") Long testId);

        @Query("SELECT ta FROM TestAnswer ta WHERE ta.attempt.id = :attemptId AND ta.answer IS NOT NULL")
        Page<TestAnswer> findAnsweredQuestionsByAttemptId(@Param("attemptId") Long attemptId, Pageable pageable);

        @Query("SELECT COUNT(ta) FROM TestAnswer ta WHERE ta.attempt.id = :attemptId")
        Long countTotalQuestionsByAttemptId(@Param("attemptId") Long attemptId);

        void deleteByAttemptId(Long attemptId);

        @Query("SELECT ta FROM TestAnswer ta JOIN FETCH ta.attempt WHERE ta.id = :id")
        Optional<TestAnswer> findByIdWithAttempt(@Param("id") Long id);

        // Performance optimization: Batch operations
        @Query("SELECT ta FROM TestAnswer ta WHERE ta.attempt.id = :attemptId AND ta.isCorrect IS NULL")
        List<TestAnswer> findUngradedAnswersByAttemptId(@Param("attemptId") Long attemptId);

}
