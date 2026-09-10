package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.ExamRequestDTO;
import com.FirstApiChallenge.api.dto.ExamResponseDTO;
import com.FirstApiChallenge.api.dto.ExamResultRequestDTO;
import com.FirstApiChallenge.api.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping("/medical-records/{medicalRecordId}")
    public ResponseEntity<ExamResponseDTO> create(
            @PathVariable Long medicalRecordId,
            @RequestParam String veterinarianCpf,
            @RequestBody @Valid ExamRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examService.create(medicalRecordId, veterinarianCpf, request));
    }

    @PutMapping("/{examId}")
    public ResponseEntity<ExamResponseDTO> update(
            @PathVariable Long examId,
            @RequestParam String veterinarianCpf,
            @RequestBody @Valid ExamRequestDTO request) {
        return ResponseEntity.ok(examService.update(examId, veterinarianCpf, request));
    }

    @PatchMapping("/{examId}/result")
    public ResponseEntity<ExamResponseDTO> registerResult(
            @PathVariable Long examId,
            @RequestParam String veterinarianCpf,
            @RequestBody @Valid ExamResultRequestDTO request) {
        return ResponseEntity.ok(examService.registerResult(examId, veterinarianCpf, request));
    }

    @PatchMapping("/{examId}/cancel")
    public ResponseEntity<ExamResponseDTO> cancel(
            @PathVariable Long examId,
            @RequestParam String veterinarianCpf) {
        return ResponseEntity.ok(examService.cancel(examId, veterinarianCpf));
    }

    @GetMapping("/tutor/{tutorCpf}/medical-records/{medicalRecordId}")
    public ResponseEntity<List<ExamResponseDTO>> getByMedicalRecordForTutor(
            @PathVariable String tutorCpf,
            @PathVariable Long medicalRecordId) {
        return ResponseEntity.ok(
                examService.getByMedicalRecordForTutor(tutorCpf, medicalRecordId)
        );
    }

    @GetMapping("/veterinarian/{veterinarianCpf}/medical-records/{medicalRecordId}")
    public ResponseEntity<List<ExamResponseDTO>> getByMedicalRecordForVeterinarian(
            @PathVariable String veterinarianCpf,
            @PathVariable Long medicalRecordId) {
        return ResponseEntity.ok(
                examService.getByMedicalRecordForVeterinarian(veterinarianCpf, medicalRecordId)
        );
    }

    @GetMapping("/tutor/{tutorCpf}/animals/{animalId}")
    public ResponseEntity<List<ExamResponseDTO>> getByAnimalForTutor(
            @PathVariable String tutorCpf,
            @PathVariable Long animalId) {
        return ResponseEntity.ok(examService.getByAnimalForTutor(tutorCpf, animalId));
    }

    @GetMapping("/veterinarian/{veterinarianCpf}/animals/{animalId}")
    public ResponseEntity<List<ExamResponseDTO>> getByAnimalForVeterinarian(
            @PathVariable String veterinarianCpf,
            @PathVariable Long animalId) {
        return ResponseEntity.ok(
                examService.getByAnimalForVeterinarian(veterinarianCpf, animalId)
        );
    }
}
