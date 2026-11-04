package com.FirstApiChallenge.api.repository;

import java.time.LocalDateTime;

public interface DashboardMetricsReader {

    TutorMetrics readTutorMetrics(
            Long tutorId,
            String tutorCpf,
            LocalDateTime referenceTime
    );

    VeterinarianMetrics readVeterinarianMetrics(
            Long veterinarianId,
            String veterinarianCpf,
            LocalDateTime referenceTime
    );

    record TutorMetrics(
            long totalPets,
            long appointmentsToday,
            long pendingAppointments,
            long linkedVeterinarians,
            long pendingExams,
            long vaccinationsAttention
    ) {
    }

    record VeterinarianMetrics(
            long linkedTutors,
            long activePatients,
            long appointmentsToday,
            long pendingAppointments,
            long confirmedAppointments,
            long completedAppointments,
            long pendingExams
    ) {
    }
}
