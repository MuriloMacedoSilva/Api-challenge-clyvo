package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;

public record ChatContactResponseDTO(
        String cpf,
        String name,
        String role,
        String crmvNumber,
        String crmvState
) {
    public static ChatContactResponseDTO fromTutor(Tutor tutor) {
        return new ChatContactResponseDTO(
                tutor.getCpf(),
                tutor.getName(),
                "tutor",
                null,
                null
        );
    }

    public static ChatContactResponseDTO fromVeterinarian(Veterinarian veterinarian) {
        return new ChatContactResponseDTO(
                veterinarian.getCpf(),
                veterinarian.getName(),
                "veterinarian",
                veterinarian.getCrmvNumber(),
                veterinarian.getCrmvState()
        );
    }
}
