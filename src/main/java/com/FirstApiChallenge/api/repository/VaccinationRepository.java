package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {

    List<Vaccination> findByAnimalIdOrderByApplicationDateDesc(Long animalId);

    List<Vaccination> findByAnimalIdAndNextDoseDateGreaterThanEqualOrderByNextDoseDateAsc(
            Long animalId,
            LocalDate date
    );

    List<Vaccination> findByAnimalIdAndNextDoseDateLessThanOrderByNextDoseDateAsc(
            Long animalId,
            LocalDate date
    );
}
