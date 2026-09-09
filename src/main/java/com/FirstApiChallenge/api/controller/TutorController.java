package com.FirstApiChallenge.api.controller;


import com.FirstApiChallenge.api.dto.AnimalDTO;
import com.FirstApiChallenge.api.dto.AnimalResponseDTO;
import com.FirstApiChallenge.api.dto.TutorRequestDTO;
import com.FirstApiChallenge.api.dto.TutorResponseDTO;
import com.FirstApiChallenge.api.service.TutorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.FirstApiChallenge.api.model.Animal;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tutor")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping(){
        return ResponseEntity.ok("ping ok!");
    }

    @PostMapping
    public ResponseEntity<TutorResponseDTO> create(@RequestBody @Valid TutorRequestDTO dto){
        TutorResponseDTO response = tutorService.createTutor(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody TutorRequestDTO dto) {
        TutorResponseDTO response;
        try {
            response = tutorService.authenticateTutor(dto);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<TutorResponseDTO> searchByCpf(@PathVariable String cpf) {
        TutorResponseDTO tutor = tutorService.searchTutorByCpf(cpf);
        return ResponseEntity.ok(tutor);
    }

    @PostMapping("/{cpf}/create-animal")
    public ResponseEntity<TutorResponseDTO> RegisterAnimal(
            @PathVariable String cpf,
            @RequestBody @Valid Set<AnimalDTO> newAnimalsDTO) {

        TutorResponseDTO response = tutorService.createAnimal(cpf, newAnimalsDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

//    @GetMapping("/{cpf}/read-animals")
//    public ResponseEntity<Set<Animal>> readAllAnimalsByCpf(@PathVariable String cpf) {
//        Set<Animal> response = tutorService.readAnimalsByTutor(cpf);
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/{cpf}/read-animals")
    public ResponseEntity<Set<AnimalResponseDTO>> readAllAnimalsByCpf(@PathVariable String cpf) {

        Set<AnimalResponseDTO> response = tutorService
                .readAnimalsByTutor(cpf)
                .stream()
                .map(AnimalResponseDTO::fromEntity)
                .collect(Collectors.toSet());

        return ResponseEntity.ok(response);
    }

//    @PutMapping("/{cpf}/animals")
//    public ResponseEntity<TutorResponseDTO> updateAnimal(
//            @PathVariable String cpf,
//            @RequestParam String originalAnimalName,
//            @RequestBody AnimalDTO updatedAnimalDTO) {
//
//        TutorResponseDTO updatedTutor = tutorService.updateAnimal(cpf, originalAnimalName, updatedAnimalDTO);
//
//        return ResponseEntity.ok(updatedTutor);
//    }

    @PutMapping("/{cpf}/animals/{animalId}")
    public ResponseEntity<TutorResponseDTO> updateAnimal(
            @PathVariable String cpf,
            @PathVariable Long animalId,
            @RequestBody @Valid AnimalDTO updatedAnimalDTO) {

        TutorResponseDTO response =
                tutorService.updateAnimal(
                        cpf,
                        animalId,
                        updatedAnimalDTO
                );

        return ResponseEntity.ok(response);
    }

//    @DeleteMapping("/{cpf}/animals")
//    public ResponseEntity<TutorResponseDTO> deleteAnimal(
//            @PathVariable String cpf,
//            @RequestParam String animalName) {
//
//        TutorResponseDTO updatedTutor = tutorService.deleteAnimal(cpf, animalName);
//        return ResponseEntity.ok(updatedTutor);
//    }

    @DeleteMapping("/{cpf}/animals/{animalId}")
    public ResponseEntity<Void> deleteAnimal(
            @PathVariable String cpf,
            @PathVariable Long animalId) {

        tutorService.deleteAnimal(cpf, animalId);

        return ResponseEntity.noContent().build();
    }

}

