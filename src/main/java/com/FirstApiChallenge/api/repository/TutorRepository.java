package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    Optional<Tutor> findByCpf(String cpf);




}