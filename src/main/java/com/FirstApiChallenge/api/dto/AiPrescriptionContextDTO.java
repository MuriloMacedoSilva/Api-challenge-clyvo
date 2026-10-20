package com.FirstApiChallenge.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AiPrescriptionContextDTO(
        Long id,
        OffsetDateTime createdAt,
        String veterinarianName,
        List<AiPrescriptionItemContextDTO> items
) {
}
