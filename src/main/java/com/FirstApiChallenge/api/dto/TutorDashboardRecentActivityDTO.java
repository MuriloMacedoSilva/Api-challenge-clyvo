package com.FirstApiChallenge.api.dto;

import java.time.LocalDateTime;

public record TutorDashboardRecentActivityDTO(
        String type,
        String title,
        String description,
        LocalDateTime occurredAt,
        Long animalId,
        String animalName,
        Long referenceId
) {
}
