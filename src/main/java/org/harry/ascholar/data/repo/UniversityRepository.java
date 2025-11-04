package org.harry.ascholar.data.repo;

import org.harry.ascholar.data.models.University;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UniversityRepository extends JpaRepository<University, Long> {
    List<University> findAllByIsVerifiedTrue();
    List<University> findAllByNameContainingIgnoreCase(String name);

    List<University> findByIsVerifiedTrue();

    List<University> findByNameContainingIgnoreCase(String name);
}
