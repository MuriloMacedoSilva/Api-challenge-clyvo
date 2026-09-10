package com.FirstApiChallenge.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExamResultRequestDTO(
        @NotBlank(message = "Resultado do exame é obrigatório")
        @Size(max = 4000, message = "Resultado deve ter no máximo 4000 caracteres")
        String result,

        @Size(max = 4000, message = "Observações devem ter no máximo 4000 caracteres")
        String observations
) {}
