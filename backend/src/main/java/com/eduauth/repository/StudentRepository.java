package com.eduauth.repository;

import com.eduauth.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Navigates the @OneToOne user relationship → users.id
    Optional<Student> findByUserId(Long userId);
}
