package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.MedicalRecord;
import com.FirstApiChallenge.api.model.Prescription;

import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionResponseDTO(
        Long id,
        Long medicalRecordId,
        Long animalId,
        String animalName,
        String veterinarianCpf,
        String veterinarianName,
        String instructions,
        List<PrescriptionItemResponseDTO> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PrescriptionResponseDTO fromEntity(Prescription prescription) {
        MedicalRecord record = prescription.getMedicalRecord();

        return new PrescriptionResponseDTO(
                prescription.getId(),
                record.getId(),
                record.getAnimal().getId(),
                record.getAnimal().getName(),
                record.getVeterinarian().getCpf(),
                record.getVeterinarian().getName(),
                prescription.getInstructions(),
                prescription.getItems().stream()
                        .map(PrescriptionItemResponseDTO::fromEntity)
                        .toList(),
                prescription.getCreatedAt(),
                prescription.getUpdatedAt()
        );
    }
}
