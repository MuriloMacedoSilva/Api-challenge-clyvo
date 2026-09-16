package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.enums.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PushTokenRequestDTO(
        @NotBlank(message = "Token push é obrigatório")
        @Size(max = 255, message = "Token push deve ter no máximo 255 caracteres")
        String token,

        @NotNull(message = "Plataforma é obrigatória")
        DevicePlatform platform
) {
}
