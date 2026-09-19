package com.eduauth.repository;

import com.eduauth.model.CertificateSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CertificateSequenceRepository extends JpaRepository<CertificateSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM CertificateSequence cs WHERE cs.prefix = :prefix AND cs.yearSuffix = :yearSuffix")
    Optional<CertificateSequence> findByPrefixAndYearSuffixWithLock(
            @Param("prefix") String prefix,
            @Param("yearSuffix") String yearSuffix);
}
