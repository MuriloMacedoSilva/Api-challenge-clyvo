package com.FirstApiChallenge.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequestDTO(
        @NotBlank(message = "Conteúdo da mensagem é obrigatório")
        @Size(max = 2000, message = "Mensagem deve ter no máximo 2000 caracteres")
        String content
) {}
