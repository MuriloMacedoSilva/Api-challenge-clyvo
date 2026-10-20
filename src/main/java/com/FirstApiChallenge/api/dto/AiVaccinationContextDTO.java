package com.FirstApiChallenge.api.dto;

import java.time.LocalDate;

public record AiVaccinationContextDTO(
        Long id,
        String vaccineName,
        LocalDate applicationDate,
        LocalDate nextDoseDate,
        String manufacturer
) {
}
