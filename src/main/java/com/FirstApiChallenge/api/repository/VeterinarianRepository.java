package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VeterinarianRepository extends JpaRepository<Veterinarian, Long> {

    Optional<Veterinarian> findByCpf(String cpf);

    Optional<Veterinarian> findByCrmvNumber(String crmvNumber);

    Optional<Tutor> findByTutors(String cpf);

}
