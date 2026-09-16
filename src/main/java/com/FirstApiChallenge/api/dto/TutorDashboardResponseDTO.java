package com.FirstApiChallenge.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TutorDashboardResponseDTO(
        long totalPets,
        long appointmentsToday,
        long pendingAppointments,
        long linkedVeterinarians,
        long pendingExams,
        long vaccinationsAttention,
        TutorDashboardNextAppointmentDTO nextAppointment,
        List<TutorDashboardRecentActivityDTO> recentActivities,
        LocalDateTime generatedAt
) {
}
