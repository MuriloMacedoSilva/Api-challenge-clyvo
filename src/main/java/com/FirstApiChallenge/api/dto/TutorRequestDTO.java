package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Veterinarian;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;


import java.util.Set;

public record TutorRequestDTO(

        Long id,

        @NotBlank(message = "nome obrigatório")
        String name,

        @NotBlank(message = "email é obrigatório")
        String email,

        @NotBlank(message = "cpf é obrigatório")
        String cpf,

        @NotNull(message = "numero de telefone é obrigatório")
        @Positive(message = "o numero de telefone tem que ser maior que zero")
        String phoneNumber,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 11, message = "O campo deve ter entre 8 e 11 caracteres")
        String password,

        @NotBlank(message = "role é obrigatório")
        String role,

        Set<Animal> animals,

        Set<Veterinarian> veterinarians

) {}
