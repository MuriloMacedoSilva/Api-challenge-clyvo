package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;

import java.time.LocalDateTime;

public record LinkResponseDTO(
        Long id,
        LinkStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String veterinarianName,
        String veterinarianCpf,
        String veterinarianCrmv
) {

    public static LinkResponseDTO fromEntity(VeterinarianTutorLink link) {
        return new LinkResponseDTO(
                link.getId(),
                link.getStatus(),
                link.getCreatedAt(),
                link.getUpdatedAt(),
                link.getVeterinarian().getName(),
                link.getVeterinarian().getCpf(),
                link.getVeterinarian().getCrmvNumber()
        );
    }
}