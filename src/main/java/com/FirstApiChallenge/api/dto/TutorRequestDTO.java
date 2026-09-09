package com.FirstApiChallenge.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TutorRequestDTO(

        Long id,

        @NotBlank(message = "nome obrigatório")
        String name,

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email deve ser válido")
        String email,

        @NotBlank(message = "cpf é obrigatório")
        String cpf,

        @NotBlank(message = "Número de telefone é obrigatório")
        @Pattern(regexp = "\\d{10,11}", message = "Número de telefone deve conter 10 ou 11 dígitos")
        String phoneNumber,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 11, message = "O campo deve ter entre 8 e 11 caracteres")
        String password,

        @NotBlank(message = "role é obrigatório")
        String role
) {}
