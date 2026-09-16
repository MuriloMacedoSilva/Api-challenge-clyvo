package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    long countByAnimalTutorCpfAndNextDoseDateLessThanEqual(String tutorCpf, LocalDate date);

    @Query("""
            select vaccination.id as referenceId,
                   vaccination.animal.id as animalId,
                   vaccination.animal.name as animalName,
                   vaccination.createdAt as occurredAt,
                   vaccination.vaccineName as detail
            from Vaccination vaccination
            where vaccination.animal.tutor.cpf = :tutorCpf
            order by vaccination.createdAt desc, vaccination.id desc
            """)
    List<RecentActivityProjection> findRecentActivitiesByTutorCpf(
            @Param("tutorCpf") String tutorCpf,
            Pageable pageable
    );

    interface RecentActivityProjection {
        Long getReferenceId();

        Long getAnimalId();

        String getAnimalName();

        LocalDateTime getOccurredAt();

        String getDetail();
    }
}
