package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.Animal;

public record AnimalResponseDTO(
        Long id,
        String name,
        Float weight,
        Float height,
        Integer age,
        String race,
        String species,
        String history
) {

    public static AnimalResponseDTO fromEntity(Animal animal) {
        return new AnimalResponseDTO(
                animal.getId(),
                animal.getName(),
                animal.getWeight(),
                animal.getHeight(),
                animal.getAge(),
                animal.getRace(),
                animal.getSpecies(),
                animal.getHistory()
        );
    }
}
