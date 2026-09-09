package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.Veterinarian;

public record LinkedVeterinarianResponseDTO(
        Long id,
        String name,
        String cpf,
        String crmvNumber,
        String crmvState
) {
    public static LinkedVeterinarianResponseDTO fromEntity(Veterinarian veterinarian) {
        return new LinkedVeterinarianResponseDTO(
                veterinarian.getId(),
                veterinarian.getName(),
                veterinarian.getCpf(),
                veterinarian.getCrmvNumber(),
                veterinarian.getCrmvState()
        );
    }
}
