package com.FirstApiChallenge.api.dto;

public record ClyvoAiResponseDTO(
        String answer,
        String disclaimer,
        String model
) {
}
