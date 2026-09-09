package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.model.Appointment;

import java.time.LocalDateTime;

public record AppointmentResponseDTO(
        Long id,
        LocalDateTime scheduledAt,
        String reason,
        AppointmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long animalId,
        String animalName,
        String tutorCpf,
        String tutorName,
        String veterinarianCpf,
        String veterinarianName
) {
    public static AppointmentResponseDTO fromEntity(Appointment appointment) {
        return new AppointmentResponseDTO(
                appointment.getId(),
                appointment.getScheduledAt(),
                appointment.getReason(),
                appointment.getStatus(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt(),
                appointment.getAnimal().getId(),
                appointment.getAnimal().getName(),
                appointment.getTutor().getCpf(),
                appointment.getTutor().getName(),
                appointment.getVeterinarian().getCpf(),
                appointment.getVeterinarian().getName()
        );
    }
}
