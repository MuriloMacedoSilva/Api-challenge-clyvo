package com.FirstApiChallenge.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiQuestionRequestDTO(
        @NotBlank(message = "A pergunta é obrigatória")
        @Size(max = 2000, message = "A pergunta deve ter no máximo 2000 caracteres")
        String question
) {
}
