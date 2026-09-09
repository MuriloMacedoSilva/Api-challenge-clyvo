package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByMedicalRecordId(Long medicalRecordId);

    boolean existsByMedicalRecordId(Long medicalRecordId);
}
