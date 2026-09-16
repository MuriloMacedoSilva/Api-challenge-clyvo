package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Optional<MedicalRecord> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    List<MedicalRecord> findByAnimalIdOrderByCreatedAtDesc(Long animalId);

    @Query("""
            select record.id as referenceId,
                   record.animal.id as animalId,
                   record.animal.name as animalName,
                   record.createdAt as occurredAt,
                   record.diagnosis as detail
            from MedicalRecord record
            where record.animal.tutor.cpf = :tutorCpf
            order by record.createdAt desc, record.id desc
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

        String getDetail();
    }
}
