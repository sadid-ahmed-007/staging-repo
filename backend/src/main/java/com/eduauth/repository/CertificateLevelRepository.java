package com.eduauth.repository;

import com.eduauth.model.CertificateLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateLevelRepository extends JpaRepository<CertificateLevel, Long> {

    List<CertificateLevel> findByInstitutionId(Long institutionId);

    List<CertificateLevel> findByInstitutionIdOrderByNameAsc(Long institutionId);

    List<CertificateLevel> findByInstitutionIdAndIsActiveTrue(Long institutionId);

    List<CertificateLevel> findByInstitutionIdAndIsActiveTrueOrderByNameAsc(Long institutionId);

    Optional<CertificateLevel> findByIdAndInstitutionId(Long id, Long institutionId);
}
