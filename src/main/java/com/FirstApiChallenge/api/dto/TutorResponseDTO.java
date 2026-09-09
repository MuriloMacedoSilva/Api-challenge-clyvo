package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;
import java.util.stream.Collectors;

public record TutorResponseDTO(
        Long id,
        String name,
        String email,
        String cpf,
        String phoneNumber,
        String password,
        String role,
        Set<AnimalResponseDTO> animals
//        @JsonIgnore
//        Set<Veterinarian> veterinarians
) {
    public static TutorResponseDTO fromEntity(Tutor tutor) {
        return new TutorResponseDTO(
                tutor.getId(),
                tutor.getName(),
                tutor.getEmail(),
                tutor.getCpf(),
                tutor.getPhoneNumber(),
                tutor.getPassword(),
                tutor.getRole(),
                tutor.getAnimals()
                        .stream()
                        .map(AnimalResponseDTO::fromEntity)
                        .collect(Collectors.toSet())
//                tutor.getVeterinarians()
        );
    }
}
