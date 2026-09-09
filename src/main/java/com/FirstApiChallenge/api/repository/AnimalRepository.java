package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByIdAndTutorCpf(Long id, String cpf);

    boolean existsByTutorIdAndNameIgnoreCase(Long tutorId, String name);
}