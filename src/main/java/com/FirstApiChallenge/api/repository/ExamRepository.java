package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.model.Exam;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Exam e WHERE e.id = :id")
    Optional<Exam> findByIdForUpdate(@Param("id") Long id);

    List<Exam> findByMedicalRecordIdOrderByRequestDateDesc(Long medicalRecordId);

    List<Exam> findByMedicalRecordAnimalIdOrderByRequestDateDesc(Long animalId);

    List<Exam> findByMedicalRecordAnimalIdAndStatusOrderByRequestDateDesc(
            Long animalId,
            ExamStatus status
    );

    long countByMedicalRecordVeterinarianCpfAndStatus(String veterinarianCpf, ExamStatus status);

    long countByMedicalRecordAnimalTutorCpfAndStatus(String tutorCpf, ExamStatus status);

    @Query("""
            select exam.id as referenceId,
                   exam.medicalRecord.animal.id as animalId,
                   exam.medicalRecord.animal.name as animalName,
                   exam.resultDate as occurredAt,
                   exam.examName as detail
            from Exam exam
            where exam.medicalRecord.animal.tutor.cpf = :tutorCpf
              and exam.status = :status
              and exam.resultDate is not null
            order by exam.resultDate desc, exam.id desc
            """)
    List<RecentActivityProjection> findRecentCompletedActivities(
            @Param("tutorCpf") String tutorCpf,
            @Param("status") ExamStatus status,
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
