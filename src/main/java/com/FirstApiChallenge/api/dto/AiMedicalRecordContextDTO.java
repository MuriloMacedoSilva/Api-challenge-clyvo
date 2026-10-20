package com.FirstApiChallenge.api.dto;

import java.time.OffsetDateTime;

public record AiMedicalRecordContextDTO(
        Long id,
        OffsetDateTime date,
        String diagnosis,
        String description,
        Float weight,
        Float temperature,
        String observations,
        String veterinarianName
) {
}
