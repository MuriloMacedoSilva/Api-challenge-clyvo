package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByMedicalRecordId(Long medicalRecordId);

    boolean existsByMedicalRecordId(Long medicalRecordId);

    @Query("""
            select prescription.id as referenceId,
                   prescription.medicalRecord.animal.id as animalId,
                   prescription.medicalRecord.animal.name as animalName,
                   prescription.createdAt as occurredAt
            from Prescription prescription
            where prescription.medicalRecord.animal.tutor.cpf = :tutorCpf
            order by prescription.createdAt desc, prescription.id desc
            """)
    List<RecentActivityProjection> findRecentActivitiesByTutorCpf(
            @Param("tutorCpf") String tutorCpf,
            Pageable pageable
    );

    interface RecentActivityProjection {
        Long getReferenceId();

        Long getAnimalId();

        String getAnimalName();

        LocalDateTime getOccurredAt();
    }
}
