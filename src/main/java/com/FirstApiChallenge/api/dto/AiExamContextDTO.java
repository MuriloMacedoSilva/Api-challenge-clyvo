package com.FirstApiChallenge.api.dto;

import java.time.OffsetDateTime;

public record AiExamContextDTO(
        Long id,
        String examName,
        String examType,
        String status,
        OffsetDateTime requestDate,
        OffsetDateTime resultDate,
        String result,
        String observations
) {
}
