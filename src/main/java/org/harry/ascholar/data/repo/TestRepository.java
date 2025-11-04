package org.harry.ascholar.data.repo;

import org.harry.ascholar.data.models.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByIsActiveTrue();

    List<Test> findByTitleContainingIgnoreCaseAndIsActiveTrue(String trim);

    long countByIsActiveTrue();
}
