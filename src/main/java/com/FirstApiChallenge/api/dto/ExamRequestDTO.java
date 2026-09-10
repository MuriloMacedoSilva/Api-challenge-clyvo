package com.FirstApiChallenge.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExamRequestDTO(
        @NotBlank(message = "Nome do exame é obrigatório")
        @Size(max = 200, message = "Nome do exame deve ter no máximo 200 caracteres")
        String examName,

        @Size(max = 200, message = "Tipo do exame deve ter no máximo 200 caracteres")
        String examType,

        @Size(max = 4000, message = "Observações devem ter no máximo 4000 caracteres")
        String observations
) {}
