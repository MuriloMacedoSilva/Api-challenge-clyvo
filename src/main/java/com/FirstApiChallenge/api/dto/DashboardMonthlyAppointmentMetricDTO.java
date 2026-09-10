package com.FirstApiChallenge.api.dto;

public record DashboardMonthlyAppointmentMetricDTO(
        int year,
        int month,
        String label,
        long count
) {
}
