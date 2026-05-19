package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.Tutor;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record VeterinarianRequestDTO(


        Long id,

        @NotBlank(message = "Nome é obrigatório")
        String name,

        @NotBlank(message = "Email é obrigatório")
        String email,

        @NotBlank(message = "CPF é obrigatório")
        String cpf,

        @NotBlank(message = "Numero de Telefone é obrigatório")
        String phoneNumber,

        @NotBlank(message = "Senha é obrigatório")
        String password,

        @NotBlank(message = "Role é obrigatório")
        String role,

        @NotBlank(message = "Numero do CRMV é obrigatório")
        String crmvNumber,

        @NotBlank(message = "Estado do CRMV é obrigatório")
        String crmvState,

        @NotBlank(message = "CNPJ é obrigatório")
        String cnpj,

        Set<Tutor> tutors

) {
}
