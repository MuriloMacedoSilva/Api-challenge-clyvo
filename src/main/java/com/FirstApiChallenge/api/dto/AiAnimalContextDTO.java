package com.FirstApiChallenge.api.dto;

public record AiAnimalContextDTO(
        Long id,
        String name,
        String species,
        String breed,
        Integer age,
        Float weight
) {
}
