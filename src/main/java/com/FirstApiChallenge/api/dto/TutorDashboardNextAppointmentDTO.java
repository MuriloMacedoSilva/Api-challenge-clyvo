package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.enums.AppointmentStatus;

import java.time.LocalDateTime;

public record TutorDashboardNextAppointmentDTO(
        Long appointmentId,
        Long animalId,
        String animalName,
        String veterinarianName,
        String veterinarianCpf,
        LocalDateTime scheduledAt,
        String reason,
        AppointmentStatus status
) {
}
