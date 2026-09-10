package com.FirstApiChallenge.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VeterinarianDashboardResponseDTO(
        long linkedTutors,
        long activePatients,
        long appointmentsToday,
        long pendingAppointments,
        long confirmedAppointments,
        long completedAppointments,
        long pendingExams,
        List<DashboardMonthlyAppointmentMetricDTO> appointmentsByMonth,
        List<DashboardSpeciesMetricDTO> patientsBySpecies,
        List<DashboardUpcomingAppointmentDTO> upcomingAppointments,
        LocalDateTime generatedAt
) {
}
