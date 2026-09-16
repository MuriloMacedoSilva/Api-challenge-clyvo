package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.model.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByIdAndTutorCpf(Long id, String cpf);

    boolean existsByTutorIdAndNameIgnoreCase(Long tutorId, String name);

    long countByTutorCpf(String tutorCpf);

    @Query("""
            select count(animal.id)
            from Animal animal
            where exists (
                select link.id
                from VeterinarianTutorLink link
                where link.tutor = animal.tutor
                  and link.veterinarian.cpf = :veterinarianCpf
                  and link.status = :status
            )
            """)
    long countByAcceptedVeterinarianLink(
            @Param("veterinarianCpf") String veterinarianCpf,
            @Param("status") LinkStatus status
    );

    @Query("""
            select animal.species as species, count(animal.id) as count
            from Animal animal
            where exists (
                select link.id
                from VeterinarianTutorLink link
                where link.tutor = animal.tutor
                  and link.veterinarian.cpf = :veterinarianCpf
                  and link.status = :status
            )
            group by animal.species
            """)
    List<SpeciesCountProjection> countBySpeciesAndAcceptedVeterinarianLink(
            @Param("veterinarianCpf") String veterinarianCpf,
            @Param("status") LinkStatus status
    );

    interface SpeciesCountProjection {
        String getSpecies();

        Long getCount();
    }
}
