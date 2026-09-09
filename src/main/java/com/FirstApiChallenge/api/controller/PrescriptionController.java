package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.PrescriptionRequestDTO;
import com.FirstApiChallenge.api.dto.PrescriptionResponseDTO;
import com.FirstApiChallenge.api.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping("/medical-records/{medicalRecordId}")
    public ResponseEntity<PrescriptionResponseDTO> create(
            @PathVariable Long medicalRecordId,
            @RequestParam String veterinarianCpf,
            @RequestBody @Valid PrescriptionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prescriptionService.create(medicalRecordId, veterinarianCpf, request));
    }

    @PutMapping("/{prescriptionId}")
    public ResponseEntity<PrescriptionResponseDTO> update(
            @PathVariable Long prescriptionId,
            @RequestParam String veterinarianCpf,
            @RequestBody @Valid PrescriptionRequestDTO request) {
        return ResponseEntity.ok(
                prescriptionService.update(prescriptionId, veterinarianCpf, request)
        );
    }

    @GetMapping("/tutor/{tutorCpf}/medical-records/{medicalRecordId}")
    public ResponseEntity<PrescriptionResponseDTO> getForTutor(
            @PathVariable String tutorCpf,
            @PathVariable Long medicalRecordId) {
        return prescriptionService.getForTutor(tutorCpf, medicalRecordId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/veterinarian/{veterinarianCpf}/medical-records/{medicalRecordId}")
    public ResponseEntity<PrescriptionResponseDTO> getForVeterinarian(
            @PathVariable String veterinarianCpf,
            @PathVariable Long medicalRecordId) {
        return prescriptionService.getForVeterinarian(veterinarianCpf, medicalRecordId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
