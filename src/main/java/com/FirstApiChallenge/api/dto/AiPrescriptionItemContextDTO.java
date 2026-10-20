package com.FirstApiChallenge.api.dto;

public record AiPrescriptionItemContextDTO(
        String medication,
        String dosage,
        String frequency,
        String duration,
        String route,
        String instructions
) {
}
