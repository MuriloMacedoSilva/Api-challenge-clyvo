package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.TutorRequestDTO;
import com.FirstApiChallenge.api.dto.TutorResponseDTO;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.repository.TutorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class TutorService {

    private final TutorRepository tutorRepository;

    public TutorService(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }


    @Transactional(readOnly = true)
    public Optional<TutorResponseDTO> searchTutorByCpf(String cpf) {
        return tutorRepository.findByCpf(cpf)
                .map(TutorResponseDTO::fromEntity);
    }

    @Transactional
    public TutorResponseDTO authenticateTutor(TutorRequestDTO requestDTO) {
        Tutor tutor = tutorRepository.findByCpf(requestDTO.cpf())
                .orElseThrow(() -> new CustomException("Usuario não encontrado", HttpStatus.NOT_FOUND));

        if (!tutor.getPassword().equals(requestDTO.password())){
            throw new CustomException("senha incorreta", HttpStatus.UNAUTHORIZED);
        }

        return TutorResponseDTO.fromEntity(tutor);
    }

    @Transactional
    public TutorResponseDTO createTutor(TutorRequestDTO requestDTO) {

        if (tutorRepository.findByCpf(requestDTO.cpf()).isPresent()) {
           throw new CustomException("CPF já cadastrado", HttpStatus.CONFLICT);
        }


        Tutor tutor = new Tutor();
        tutor.setName(requestDTO.name());
        tutor.setEmail(requestDTO.email());
        tutor.setCpf(requestDTO.cpf());
        tutor.setPhoneNumber(requestDTO.phoneNumber());
        tutor.setPassword(requestDTO.password());
        tutor.setRole(requestDTO.role());
        tutor.setAnimals(requestDTO.animals());
        tutor.setVeterinarians(requestDTO.veterinarians());


        Tutor tutorSaved = tutorRepository.save(tutor);

        return TutorResponseDTO.fromEntity(tutorSaved);
    }

    @Transactional
    public Optional<TutorResponseDTO> updateTutorByCpf(String cpf, TutorRequestDTO requestDTO) {

        if (tutorRepository.findByCpf(requestDTO.cpf()).isEmpty()) {
            throw new CustomException("CPF não encontrado", HttpStatus.NOT_FOUND);
        }

        return tutorRepository.findByCpf(cpf).map(tutorExistent -> {
            tutorExistent.setName(requestDTO.name());
            tutorExistent.setEmail(requestDTO.email());
            tutorExistent.setPhoneNumber(requestDTO.phoneNumber());
            tutorExistent.setPassword(requestDTO.password());
            tutorExistent.setRole(requestDTO.role());
            tutorExistent.setAnimals(requestDTO.animals());
            tutorExistent.setVeterinarians(requestDTO.veterinarians());

            Tutor tutorUpdated = tutorRepository.save(tutorExistent);

            return TutorResponseDTO.fromEntity(tutorUpdated);
        }
        );
    }

    @Transactional
    public TutorResponseDTO createAnimal(String cpf, Set<Animal> newAnimal) {
        Tutor tutor = tutorRepository.findByCpf(cpf)
                .orElseThrow(() -> new CustomException("Tutor não encointrado", HttpStatus.NOT_FOUND));

        for (Animal animal : tutor.getAnimals()) {
            for (Animal animalNew : newAnimal) {
                if (animal.getName().equals(animalNew.getName())) {
                    throw new CustomException("Este animal já está cadastrado", HttpStatus.CONFLICT);
                } else {
                    tutor.setAnimals(newAnimal);
                }
            }
        }

        tutorRepository.save(tutor);

        return TutorResponseDTO.fromEntity(tutor);

    }

    @Transactional
    public Set<Animal> readAnimalsByTutor(String cpf) {
        Tutor tutor = tutorRepository.findByCpf(cpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));

        Set<Animal> animals = tutor.getAnimals();

        if (animals.isEmpty()){
            throw new CustomException("Não há animais cadastrados", HttpStatus.NOT_FOUND);
        }

        return animals;
    }

}
