package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.model.Exam;
import com.FirstApiChallenge.api.model.MedicalRecord;

import java.time.LocalDateTime;

public record ExamResponseDTO(
        Long id,
        Long medicalRecordId,
        Long animalId,
        String animalName,
        String veterinarianCpf,
        String veterinarianName,
        String examName,
        String examType,
        ExamStatus status,
        LocalDateTime requestDate,
        LocalDateTime resultDate,
        String result,
        String observations,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExamResponseDTO fromEntity(Exam exam) {
        MedicalRecord record = exam.getMedicalRecord();

        return new ExamResponseDTO(
                exam.getId(),
                record.getId(),
                record.getAnimal().getId(),
                record.getAnimal().getName(),
                record.getVeterinarian().getCpf(),
                record.getVeterinarian().getName(),
                exam.getExamName(),
                exam.getExamType(),
                exam.getStatus(),
                exam.getRequestDate(),
                exam.getResultDate(),
                exam.getResult(),
                exam.getObservations(),
                exam.getCreatedAt(),
                exam.getUpdatedAt()
        );
    }
}
