package com.eduauth.repository;

import com.eduauth.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Navigates the @OneToOne user relationship → users.id
    Optional<Student> findByUserId(Long userId);

    @Query("SELECT s FROM Student s JOIN FETCH s.user WHERE s.user.id = :userId")
    Optional<Student> findByUserIdWithUser(@Param("userId") Long userId);

    @Query("SELECT s FROM Student s JOIN FETCH s.user WHERE s.id = :id")
    Optional<Student> findByIdWithUser(@Param("id") Long id);

    // Find student by their account email (joins through user)
    @Query("SELECT s FROM Student s JOIN FETCH s.user WHERE s.user.email = :email")
    Optional<Student> findByUserEmail(@Param("email") String email);

    /** Search students by name or email for enrollment */
    @Query("""
            SELECT s FROM Student s
            WHERE s.user.role = 'student'
            AND s.user.isApproved = true
            AND s.deletedAt IS NULL
            AND (
                LOWER(s.firstName) LIKE LOWER(CONCAT('%',:q,'%'))
                OR LOWER(s.lastName) LIKE LOWER(CONCAT('%',:q,'%'))
                OR LOWER(s.user.email) LIKE LOWER(CONCAT('%',:q,'%'))
            )
            """)
    List<Student> searchApprovedStudents(@Param("q") String q);

    /** Find a student by their SHA-256 hashed NID — used by verifier search. */
    @Query("SELECT s FROM Student s JOIN FETCH s.user WHERE s.nidHash = :nidHash")
    Optional<Student> findByNidHash(@Param("nidHash") String nidHash);
}
