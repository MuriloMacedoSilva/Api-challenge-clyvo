package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.AnimalDTO;
import com.FirstApiChallenge.api.dto.TutorRequestDTO;
import com.FirstApiChallenge.api.dto.TutorResponseDTO;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.repository.TutorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TutorService {

    private final TutorRepository tutorRepository;

    public TutorService(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }


    @Transactional(readOnly = true)
    public TutorResponseDTO searchTutorByCpf(String cpf) {
        return tutorRepository.findByCpf(cpf)
                .map(TutorResponseDTO::fromEntity)
                .orElseThrow(() -> new CustomException("Tutor não encontrado para o CPF informado.", HttpStatus.NOT_FOUND));
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
        }).or(() -> {
            throw new CustomException("CPF não encontrado: " + cpf, HttpStatus.NOT_FOUND);
        });
    }

    @Transactional
    public TutorResponseDTO createAnimal(String cpf, Set<AnimalDTO> newAnimalsDTO) {
        Tutor tutor = tutorRepository.findByCpf(cpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));

        // Nomes já existentes
        Set<String> existingNames = tutor.getAnimals().stream()
                .map(animal -> animal.getName().trim().toLowerCase())
                .collect(Collectors.toSet());

        // Converte os DTOs para Entidade e valida duplicidade
        Set<Animal> newAnimals = new HashSet<>();

        for (AnimalDTO dto : newAnimalsDTO) {
            String normalizedNewName = dto.name().trim().toLowerCase();

            if (existingNames.contains(normalizedNewName)) {
                throw new CustomException(
                        "O animal '" + dto.name() + "' já está cadastrado para este tutor",
                        HttpStatus.CONFLICT
                );
            }

            newAnimals.add(dto.toEntity());
        }

        // Adiciona e salva no banco
        tutor.getAnimals().addAll(newAnimals);
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

    @Transactional
    public TutorResponseDTO updateAnimal(String cpf, String originalAnimalName, AnimalDTO updatedAnimalDTO) {

        // 1. Busca o tutor
        Tutor tutor = tutorRepository.findByCpf(cpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));

        String normalizedOriginalName = originalAnimalName.trim().toLowerCase();

        // 2. Encontra o animal existente pelo nome original
        Animal existingAnimal = tutor.getAnimals().stream()
                .filter(a -> a.getName().trim().toLowerCase().equals(normalizedOriginalName))
                .findFirst()
                .orElseThrow(() -> new CustomException("Animal '" + originalAnimalName + "' não encontrado", HttpStatus.NOT_FOUND));

        String normalizedNewName = updatedAnimalDTO.name().trim().toLowerCase();

        // 3. Se o tutor estiver tentando mudar o nome do animal, valida se o novo nome já não existe
        if (!normalizedOriginalName.equals(normalizedNewName)) {
            boolean nameConflict = tutor.getAnimals().stream()
                    .anyMatch(a -> a.getName().trim().toLowerCase().equals(normalizedNewName));

            if (nameConflict) {
                throw new CustomException(
                        "Já existe outro animal cadastrado com o nome '" + updatedAnimalDTO.name() + "'",
                        HttpStatus.CONFLICT
                );
            }
        }

        // 4. Remove o animal antigo do Set e adiciona o novo (Estratégia segura para @ElementCollection)
        tutor.getAnimals().remove(existingAnimal);
        tutor.getAnimals().add(updatedAnimalDTO.toEntity());

        // 5. Salva o tutor
        Tutor tutorSaved = tutorRepository.save(tutor);

        return TutorResponseDTO.fromEntity(tutorSaved);
    }

    @Transactional
    public TutorResponseDTO deleteAnimal(String cpf, String animalName) {
        // 1. Busca o tutor pelo CPF
        Tutor tutor = tutorRepository.findByCpf(cpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));

        String normalizedName = animalName.trim().toLowerCase();

        // 2. Localiza o animal na lista
        Animal animalToDelete = tutor.getAnimals().stream()
                .filter(a -> a.getName().trim().toLowerCase().equals(normalizedName))
                .findFirst()
                .orElseThrow(() -> new CustomException("Animal '" + animalName + "' não encontrado", HttpStatus.NOT_FOUND));

        // 3. Remove o objeto embedado da coleção
        tutor.getAnimals().remove(animalToDelete);

        // 4. Sincroniza e salva o estado no banco de dados
        Tutor tutorSaved = tutorRepository.save(tutor);

        return TutorResponseDTO.fromEntity(tutorSaved);
    }
}
