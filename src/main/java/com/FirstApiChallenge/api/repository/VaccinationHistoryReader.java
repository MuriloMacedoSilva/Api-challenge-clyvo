package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.dto.VaccinationResponseDTO;

import java.util.List;

public interface VaccinationHistoryReader {

    List<VaccinationResponseDTO> findByAnimalId(Long animalId);
}
