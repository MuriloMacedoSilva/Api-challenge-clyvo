package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.VaccinationRequestDTO;
import com.FirstApiChallenge.api.dto.VaccinationResponseDTO;
import com.FirstApiChallenge.api.service.VaccinationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/vaccinations")
public class VaccinationController {

    private final VaccinationService vaccinationService;

    public VaccinationController(VaccinationService vaccinationService) {
        this.vaccinationService = vaccinationService;
    }

    @PostMapping("/animals/{animalId}")
    public ResponseEntity<VaccinationResponseDTO> create(
            @PathVariable Long animalId,
            @RequestParam String veterinarianCpf,
            @RequestBody @Valid VaccinationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vaccinationService.create(animalId, veterinarianCpf, request));
    }

    @PutMapping("/{vaccinationId}")
    public ResponseEntity<VaccinationResponseDTO> update(
            @PathVariable Long vaccinationId,
            @RequestParam String veterinarianCpf,
            @RequestBody @Valid VaccinationRequestDTO request) {
        return ResponseEntity.ok(vaccinationService.update(vaccinationId, veterinarianCpf, request));
    }

    @GetMapping("/tutor/{tutorCpf}/animals/{animalId}")
    public ResponseEntity<List<VaccinationResponseDTO>> getByAnimalForTutor(
            @PathVariable String tutorCpf,
            @PathVariable Long animalId) {
        return ResponseEntity.ok(vaccinationService.getByAnimalForTutor(tutorCpf, animalId));
    }

    @GetMapping("/veterinarian/{veterinarianCpf}/animals/{animalId}")
    public ResponseEntity<List<VaccinationResponseDTO>> getByAnimalForVeterinarian(
            @PathVariable String veterinarianCpf,
            @PathVariable Long animalId) {
        return ResponseEntity.ok(
                vaccinationService.getByAnimalForVeterinarian(veterinarianCpf, animalId)
        );
    }
}
