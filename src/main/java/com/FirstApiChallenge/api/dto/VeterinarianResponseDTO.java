package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record VeterinarianResponseDTO(
        Long id,
        String name,
        String email,
        String cpf,
        String phoneNumber,
        String password,
        String role,
        String crmvNumber,
        String crmvState,
        String cnpj,
        Set<Tutor> tutors
) {
    public static VeterinarianResponseDTO fromEntity (Veterinarian veterinarian) {
        return new VeterinarianResponseDTO(
                veterinarian.getId(),
                veterinarian.getName(),
                veterinarian.getEmail(),
                veterinarian.getCpf(),
                veterinarian.getPhoneNumber(),
                veterinarian.getPassword(),
                veterinarian.getRole(),
                veterinarian.getCrmvNumber(),
                veterinarian.getCrmvState(),
                veterinarian.getCnpj(),
                veterinarian.getTutors());
    }
}
