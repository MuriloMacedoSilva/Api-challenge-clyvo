package com.FirstApiChallenge.api.controler;


import com.FirstApiChallenge.api.dto.TutorRequestDTO;
import com.FirstApiChallenge.api.dto.TutorResponseDTO;
import com.FirstApiChallenge.api.service.TutorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.FirstApiChallenge.api.model.Animal;

import java.util.Set;

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

    @PostMapping("/Login")
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
    public ResponseEntity<TutorResponseDTO> searchTutorByCpf(@PathVariable String cpf){
        return tutorService.searchTutorByCpf(cpf)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{cpf}/CreateAnimal")
    public ResponseEntity<TutorResponseDTO> createAnimal(@RequestBody @Valid TutorRequestDTO requestDTO) {
        TutorResponseDTO response = tutorService.createAnimal(requestDTO.cpf(), requestDTO.animals());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{cpf}/ReadAnimals")
    public ResponseEntity<Set<Animal>> readAllAnimalsByCpf(TutorRequestDTO requestDTO) {
        Set<Animal> response = tutorService.readAnimalsByTutor(requestDTO.cpf());

        return ResponseEntity.ok(response);
    }


}

