package org.harry.ascholar.data.repo;

import org.harry.ascholar.data.models.User;
import org.harry.ascholar.data.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByUniversityId(Long universityId);
    List<User> findByRole(UserRole role);
    long countByIsEnabledTrue();
    long countByEmailVerifiedTrue();

    @Query("SELECT u FROM User u WHERE u.isEnabled = true AND u.emailVerified = true")
    List<User> findActiveVerifiedUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.university.id = :universityId AND u.role = 'UNIVERSITY_ADMIN'")
    long countUniversityAdmins(@Param("universityId") Long universityId);
}