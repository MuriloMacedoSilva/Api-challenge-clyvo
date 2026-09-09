package com.FirstApiChallenge.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescriptionItemRequestDTO(
        @NotBlank(message = "Nome do medicamento é obrigatório")
        @Size(max = 200, message = "Nome do medicamento deve ter no máximo 200 caracteres")
        String medicationName,

        @NotBlank(message = "Dosagem é obrigatória")
        @Size(max = 200, message = "Dosagem deve ter no máximo 200 caracteres")
        String dosage,

        @NotBlank(message = "Frequência é obrigatória")
        @Size(max = 200, message = "Frequência deve ter no máximo 200 caracteres")
        String frequency,

        @NotBlank(message = "Duração é obrigatória")
        @Size(max = 200, message = "Duração deve ter no máximo 200 caracteres")
        String duration,

        @NotBlank(message = "Via de administração é obrigatória")
        @Size(max = 100, message = "Via de administração deve ter no máximo 100 caracteres")
        String route,

        @Size(max = 1000, message = "Instruções devem ter no máximo 1000 caracteres")
        String instructions
) {}
