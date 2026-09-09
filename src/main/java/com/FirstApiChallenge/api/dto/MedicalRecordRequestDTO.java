package com.FirstApiChallenge.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MedicalRecordRequestDTO(
        @NotBlank(message = "Diagnóstico é obrigatório")
        @Size(max = 500, message = "Diagnóstico deve ter no máximo 500 caracteres")
        String diagnosis,

        @NotBlank(message = "Descrição clínica é obrigatória")
        @Size(max = 4000, message = "Descrição clínica deve ter no máximo 4000 caracteres")
        String description,

        @Positive(message = "Peso deve ser maior que zero")
        Float weight,

        @Positive(message = "Temperatura deve ser maior que zero")
        Float temperature,

        @Size(max = 4000, message = "Observações devem ter no máximo 4000 caracteres")
        String observations
) {}
