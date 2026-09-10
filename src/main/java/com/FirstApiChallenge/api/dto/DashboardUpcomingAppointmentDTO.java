package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.enums.AppointmentStatus;

import java.time.LocalDateTime;

public record DashboardUpcomingAppointmentDTO(
        Long appointmentId,
        Long animalId,
        String animalName,
        String tutorName,
        LocalDateTime scheduledAt,
        String reason,
        AppointmentStatus status
) {
}
