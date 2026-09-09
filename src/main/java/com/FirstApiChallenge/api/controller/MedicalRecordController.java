package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.MedicalRecordRequestDTO;
import com.FirstApiChallenge.api.dto.MedicalRecordResponseDTO;
import com.FirstApiChallenge.api.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @PostMapping("/appointments/{appointmentId}")
    public ResponseEntity<MedicalRecordResponseDTO> create(
            @PathVariable Long appointmentId,
            @RequestParam String veterinarianCpf,
            @RequestBody @Valid MedicalRecordRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalRecordService.create(appointmentId, veterinarianCpf, request));
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<MedicalRecordResponseDTO> update(
            @PathVariable Long recordId,
            @RequestParam String veterinarianCpf,
            @RequestBody @Valid MedicalRecordRequestDTO request) {
        return ResponseEntity.ok(medicalRecordService.update(recordId, veterinarianCpf, request));
    }

    @GetMapping("/tutor/{tutorCpf}/animals/{animalId}")
    public ResponseEntity<List<MedicalRecordResponseDTO>> getForTutor(
            @PathVariable String tutorCpf,
            @PathVariable Long animalId) {
        return ResponseEntity.ok(
                medicalRecordService.getAnimalHistoryForTutor(tutorCpf, animalId)
        );
    }

    @GetMapping("/veterinarian/{veterinarianCpf}/animals/{animalId}")
    public ResponseEntity<List<MedicalRecordResponseDTO>> getForVeterinarian(
            @PathVariable String veterinarianCpf,
            @PathVariable Long animalId) {
        return ResponseEntity.ok(
                medicalRecordService.getAnimalHistoryForVeterinarian(veterinarianCpf, animalId)
        );
    }
}
