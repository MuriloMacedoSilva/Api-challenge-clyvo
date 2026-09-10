package com.FirstApiChallenge.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record VaccinationRequestDTO(
        @NotBlank(message = "Nome da vacina é obrigatório")
        @Size(max = 200, message = "Nome da vacina deve ter no máximo 200 caracteres")
        String vaccineName,

        @NotNull(message = "Data de aplicação é obrigatória")
        @PastOrPresent(message = "Data de aplicação não pode estar no futuro")
        LocalDate applicationDate,

        LocalDate nextDoseDate,

        @Size(max = 100, message = "Lote deve ter no máximo 100 caracteres")
        String batchNumber,

        @Size(max = 200, message = "Fabricante deve ter no máximo 200 caracteres")
        String manufacturer,

        @Size(max = 4000, message = "Observações devem ter no máximo 4000 caracteres")
        String observations
) {}
