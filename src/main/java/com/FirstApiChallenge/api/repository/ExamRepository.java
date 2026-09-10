package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.model.Exam;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
}
