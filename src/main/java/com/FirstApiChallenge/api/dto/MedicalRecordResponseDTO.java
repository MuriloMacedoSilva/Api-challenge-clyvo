package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.MedicalRecord;

import java.time.LocalDateTime;

public record MedicalRecordResponseDTO(
        Long id,
        Long appointmentId,
        Long animalId,
        String animalName,
        String veterinarianCpf,
        String veterinarianName,
        String diagnosis,
        String description,
        Float weight,
        Float temperature,
        String observations,
        LocalDateTime appointmentScheduledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MedicalRecordResponseDTO fromEntity(MedicalRecord record) {
        return new MedicalRecordResponseDTO(
                record.getId(),
                record.getAppointment().getId(),
                record.getAnimal().getId(),
                record.getAnimal().getName(),
                record.getVeterinarian().getCpf(),
                record.getVeterinarian().getName(),
                record.getDiagnosis(),
                record.getDescription(),
                record.getWeight(),
                record.getTemperature(),
                record.getObservations(),
                record.getAppointment().getScheduledAt(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
