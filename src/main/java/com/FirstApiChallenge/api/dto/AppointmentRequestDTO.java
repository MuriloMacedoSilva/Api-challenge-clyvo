package com.FirstApiChallenge.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AppointmentRequestDTO(
        @NotNull(message = "Animal é obrigatório")
        Long animalId,

        @NotBlank(message = "Veterinário é obrigatório")
        String veterinarianCpf,

        @NotNull(message = "Data e horário são obrigatórios")
        @Future(message = "A data da consulta deve estar no futuro")
        LocalDateTime scheduledAt,

        @NotBlank(message = "Motivo da consulta é obrigatório")
        @Size(max = 1000, message = "Motivo da consulta deve ter no máximo 1000 caracteres")
        String reason
) {}
