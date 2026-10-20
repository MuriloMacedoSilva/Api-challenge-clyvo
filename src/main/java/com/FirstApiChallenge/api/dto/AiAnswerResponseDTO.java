package com.FirstApiChallenge.api.dto;

public record AiAnswerResponseDTO(
        String answer,
        String disclaimer,
        String model
) {
}
