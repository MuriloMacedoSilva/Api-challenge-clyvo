package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.Vaccination;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VaccinationResponseDTO(
        Long id,
        Long animalId,
        String animalName,
        String veterinarianCpf,
        String veterinarianName,
        String veterinarianCrmvNumber,
        String veterinarianCrmvState,
        String vaccineName,
        LocalDate applicationDate,
        LocalDate nextDoseDate,
        String batchNumber,
        String manufacturer,
        String observations,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VaccinationResponseDTO fromEntity(Vaccination vaccination) {
        return new VaccinationResponseDTO(
                vaccination.getId(),
                vaccination.getAnimal().getId(),
                vaccination.getAnimal().getName(),
                vaccination.getVeterinarian().getCpf(),
                vaccination.getVeterinarian().getName(),
                vaccination.getVeterinarian().getCrmvNumber(),
                vaccination.getVeterinarian().getCrmvState(),
                vaccination.getVaccineName(),
                vaccination.getApplicationDate(),
                vaccination.getNextDoseDate(),
                vaccination.getBatchNumber(),
                vaccination.getManufacturer(),
                vaccination.getObservations(),
                vaccination.getCreatedAt(),
                vaccination.getUpdatedAt()
        );
    }
}
