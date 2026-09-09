package com.FirstApiChallenge.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PrescriptionRequestDTO(
        @Size(max = 4000, message = "Orientações gerais devem ter no máximo 4000 caracteres")
        String instructions,

        @NotEmpty(message = "A prescrição deve possuir pelo menos um medicamento")
        List<@Valid PrescriptionItemRequestDTO> items
) {}
