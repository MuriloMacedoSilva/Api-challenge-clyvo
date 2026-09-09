package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VeterinarianRepository extends JpaRepository<Veterinarian, Long> {

    Optional<Veterinarian> findByCpf(String cpf);

    Optional<Veterinarian> findByCrmvNumber(String crmvNumber);

//    List<Veterinarian> findByTutorsCpf(String cpf);

//    @Query("SELECT v.tutors FROM Veterinarian v WHERE v.cpf = :cpf")
//    List<Tutor> findTutorsByVeterinarianCpf(@Param("cpf") String cpf);

}
