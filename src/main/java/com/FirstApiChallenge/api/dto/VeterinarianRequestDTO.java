package com.FirstApiChallenge.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VeterinarianRequestDTO(


        Long id,

        @NotBlank(message = "Nome é obrigatório")
        String name,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        String email,

        @NotBlank(message = "CPF é obrigatório")
        String cpf,

        @NotBlank(message = "Número de telefone é obrigatório")
        @Pattern(regexp = "\\d{10,11}", message = "Número de telefone deve conter 10 ou 11 dígitos")
        String phoneNumber,

        @NotBlank(message = "Senha é obrigatório")
        @Size(min = 8, max = 11, message = "A senha deve ter entre 8 e 11 caracteres")
        String password,

        @NotBlank(message = "Role é obrigatório")
        String role,

        @NotBlank(message = "Numero do CRMV é obrigatório")
        String crmvNumber,

        @NotBlank(message = "Estado do CRMV é obrigatório")
        String crmvState,

        @NotBlank(message = "CNPJ é obrigatório")
        String cnpj

) {
}
