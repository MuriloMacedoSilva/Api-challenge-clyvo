package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.AnimalDTO;
import com.FirstApiChallenge.api.dto.TutorResponseDTO;
import com.FirstApiChallenge.api.dto.VeterinarianRequestDTO;
import com.FirstApiChallenge.api.dto.VeterinarianResponseDTO;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.FirstApiChallenge.api.enums.LinkStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VeterinarianService {

    private final VeterinarianRepository veterinarianRepository;

    private final TutorRepository tutorRepository;

    private final AnimalRepository animalRepository;

    private final VeterinarianTutorLinkRepository veterinarianTutorLinkRepository;

    public VeterinarianService(VeterinarianRepository veterinarianRepository, TutorRepository tutorRepository, AnimalRepository animalRepository, VeterinarianTutorLinkRepository veterinarianTutorLinkRepository) {
        this.veterinarianRepository = veterinarianRepository;
        this.tutorRepository = tutorRepository;
        this.animalRepository = animalRepository;
        this.veterinarianTutorLinkRepository = veterinarianTutorLinkRepository;
    }

    @Transactional(readOnly = true)
    public Optional<VeterinarianResponseDTO> searchVeterinarianByCpf(String cpf) {
        return veterinarianRepository.findByCpf(cpf)
                .map(VeterinarianResponseDTO::fromEntity);
    }

    @Transactional
    public VeterinarianResponseDTO authenticateVeterinarian(VeterinarianRequestDTO requestDTO) {
        Veterinarian veterinarian = veterinarianRepository.findByCpf(requestDTO.cpf())
                .orElseThrow(() -> new RuntimeException("Veterinario não encontrado"));

        if (!veterinarian.getPassword().equals(requestDTO.password())){
            throw new RuntimeException("Senha incorreta");
        }

        return VeterinarianResponseDTO.fromEntity(veterinarian);
    }


    @Transactional
    public VeterinarianResponseDTO createVeterinarian(VeterinarianRequestDTO requestDTO) {

        if (veterinarianRepository.findByCpf(requestDTO.cpf()).isPresent()) {
            throw new CustomException("CPF já cadastrado", HttpStatus.CONFLICT);
        }

        Veterinarian veterinarian = new Veterinarian();
        veterinarian.setName(requestDTO.name());
        veterinarian.setEmail(requestDTO.email());
        veterinarian.setCpf(requestDTO.cpf());
        veterinarian.setCnpj(requestDTO.cnpj());
        veterinarian.setPassword(requestDTO.password());
        veterinarian.setPhoneNumber(requestDTO.phoneNumber());
        veterinarian.setCrmvNumber(requestDTO.crmvNumber());
        veterinarian.setCrmvState(requestDTO.crmvState());
        veterinarian.setRole(requestDTO.role());

        Veterinarian veterinarianSaved = veterinarianRepository.save(veterinarian);

        return VeterinarianResponseDTO.fromEntity(veterinarianSaved);

    }


    @Transactional
    public Optional<VeterinarianResponseDTO> updateVeterinarianByCpf(String cpf, VeterinarianRequestDTO requestDTO) {
        return veterinarianRepository.findByCpf(cpf).map(veterinarianExistent -> {
            veterinarianExistent.setName(requestDTO.name());
            veterinarianExistent.setEmail(requestDTO.email());
            veterinarianExistent.setCpf(requestDTO.cpf());
            veterinarianExistent.setCnpj(requestDTO.cnpj());
            veterinarianExistent.setPassword(requestDTO.password());
            veterinarianExistent.setPhoneNumber(requestDTO.phoneNumber());
            veterinarianExistent.setCrmvNumber(requestDTO.crmvNumber());
            veterinarianExistent.setCrmvState(requestDTO.crmvState());
            veterinarianExistent.setRole(requestDTO.role());

            Veterinarian veterinarianUpdated = veterinarianRepository.save(veterinarianExistent);

            return VeterinarianResponseDTO.fromEntity(veterinarianUpdated);

        });
    }

    @Transactional(readOnly = true)
    public List<TutorResponseDTO> findTutorsByVeterinarianCpf(String cpf) {

        Veterinarian veterinarian = veterinarianRepository.findByCpf(cpf)
                .orElseThrow(() -> new CustomException("Veterinário não encontrado", HttpStatus.NOT_FOUND));

//        return veterinarian.getTutors().stream()
//                .map(TutorResponseDTO::fromEntity)
//                .collect(Collectors.toList());

        return veterinarianTutorLinkRepository
                .findByVeterinarianCpfAndStatus(cpf, LinkStatus.ACCEPTED)
                .stream()
                .map(link -> TutorResponseDTO.fromEntity(link.getTutor()))
                .collect(Collectors.toList());
    }

//    @Transactional
//    public TutorResponseDTO updateAnimal(String cpf, String originalAnimalName, AnimalDTO updatedAnimalDTO) {
//
//        String normalizedOriginalName = originalAnimalName.trim().toLowerCase();
//
//        // 1. Busca o tutor
//        Tutor tutor = veterinarianRepository.findTutorsByVeterinarianCpf(cpf)
//                .stream().filter(t -> t.getAnimals().stream().anyMatch(a -> a.getName().trim().toLowerCase().equals(normalizedOriginalName)))
//                .findFirst()
//                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));
//
//
//
//        // 2. Encontra o animal existente pelo nome original
//        Animal existingAnimal = tutor.getAnimals().stream()
//                .filter(a -> a.getName().trim().toLowerCase().equals(normalizedOriginalName))
//                .findFirst()
//                .orElseThrow(() -> new CustomException("Animal '" + originalAnimalName + "' não encontrado", HttpStatus.NOT_FOUND));
//
//        String normalizedNewName = updatedAnimalDTO.name().trim().toLowerCase();
//
//        // 3. Se o tutor estiver tentando mudar o nome do animal, valida se o novo nome já não existe
//        if (!normalizedOriginalName.equals(normalizedNewName)) {
//            boolean nameConflict = tutor.getAnimals().stream()
//                    .anyMatch(a -> a.getName().trim().toLowerCase().equals(normalizedNewName));
//
//            if (nameConflict) {
//                throw new CustomException(
//                        "Já existe outro animal cadastrado com o nome '" + updatedAnimalDTO.name() + "'",
//                        HttpStatus.CONFLICT
//                );
//            }
//        }
//
//        // 4. Remove o animal antigo do set e adiciona o novo (Estratégia segura para @ElementCollection)
//        tutor.getAnimals().remove(existingAnimal);
//        tutor.getAnimals().add(updatedAnimalDTO.toEntity());
//
//        // 5. Salva o tutor
//        Tutor tutorSaved = tutorRepository.save(tutor);
//
//        return TutorResponseDTO.fromEntity(tutorSaved);
//    }



    @Transactional
    public TutorResponseDTO updateAnimal(
            String cpf,
            Long animalId,
            AnimalDTO updatedAnimalDTO
    ) {

        // 1. Verifica se o veterinário existe
        veterinarianRepository.findByCpf(cpf)
                .orElseThrow(() ->
                        new CustomException(
                                "Veterinário não encontrado",
                                HttpStatus.NOT_FOUND
                        )
                );

        // 2. Busca o animal pelo ID
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() ->
                        new CustomException(
                                "Animal não encontrado",
                                HttpStatus.NOT_FOUND
                        )
                );

        Tutor tutor = animal.getTutor();

        // 3. Verifica se o veterinário possui vínculo com o tutor
//        boolean hasTutor = veterinarianRepository
//                .findTutorsByVeterinarianCpf(cpf)
//                .stream()
//                .anyMatch(t -> t.getId().equals(tutor.getId()));
//
//        if (!hasTutor) {
//            throw new CustomException(
//                    "O veterinário não possui acesso a este tutor",
//                    HttpStatus.FORBIDDEN
//            );
//        }

        boolean hasAcceptedLink =
                veterinarianTutorLinkRepository
                        .existsByVeterinarianCpfAndTutorCpfAndStatus(
                                cpf,
                                tutor.getCpf(),
                                LinkStatus.ACCEPTED
                        );

        if (!hasAcceptedLink) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Veterinário não possui vínculo aceito com este tutor"
            );
        }

        // 4. Verifica conflito de nome
        boolean nameConflict = tutor.getAnimals().stream()
                .anyMatch(a ->
                        !a.getId().equals(animalId) &&
                                a.getName().trim()
                                        .equalsIgnoreCase(updatedAnimalDTO.name().trim())
                );

        if (nameConflict) {
            throw new CustomException(
                    "Já existe outro animal cadastrado com o nome '"
                            + updatedAnimalDTO.name() + "'",
                    HttpStatus.CONFLICT
            );
        }

        // 5. Atualiza o animal existente
        animal.setName(updatedAnimalDTO.name());
        animal.setWeight(updatedAnimalDTO.weight());
        animal.setHeight(updatedAnimalDTO.height());
        animal.setAge(updatedAnimalDTO.age());
        animal.setRace(updatedAnimalDTO.race());
        animal.setSpecies(updatedAnimalDTO.species());
        animal.setHistory(updatedAnimalDTO.history());

        // 6. Salva
        animalRepository.save(animal);

        return TutorResponseDTO.fromEntity(tutor);
    }


}
