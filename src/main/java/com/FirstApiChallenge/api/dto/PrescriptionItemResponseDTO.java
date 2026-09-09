package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.PrescriptionItem;

public record PrescriptionItemResponseDTO(
        Long id,
        String medicationName,
        String dosage,
        String frequency,
        String duration,
        String route,
        String instructions
) {
    public static PrescriptionItemResponseDTO fromEntity(PrescriptionItem item) {
        return new PrescriptionItemResponseDTO(
                item.getId(),
                item.getMedicationName(),
                item.getDosage(),
                item.getFrequency(),
                item.getDuration(),
                item.getRoute(),
                item.getInstructions()
        );
    }
}
