package com.FirstApiChallenge.api.dto;

import java.time.OffsetDateTime;

public record AiAppointmentContextDTO(
        Long id,
        OffsetDateTime scheduledAt,
        String reason,
        String status,
        String veterinarianName
) {
}
