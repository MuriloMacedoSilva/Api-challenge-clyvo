package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.dto.VaccinationResponseDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!oracle")
public class JpaVaccinationHistoryReader implements VaccinationHistoryReader {

    private final VaccinationRepository vaccinationRepository;

    public JpaVaccinationHistoryReader(VaccinationRepository vaccinationRepository) {
        this.vaccinationRepository = vaccinationRepository;
    }

    @Override
    public List<VaccinationResponseDTO> findByAnimalId(Long animalId) {
        return vaccinationRepository.findByAnimalIdOrderByApplicationDateDescIdDesc(animalId)
                .stream()
                .map(VaccinationResponseDTO::fromEntity)
                .toList();
    }
}
