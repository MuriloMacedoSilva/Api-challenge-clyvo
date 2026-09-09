package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.AppointmentRequestDTO;
import com.FirstApiChallenge.api.dto.AppointmentResponseDTO;
import com.FirstApiChallenge.api.dto.LinkedVeterinarianResponseDTO;
import com.FirstApiChallenge.api.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/tutor/{tutorCpf}")
    public ResponseEntity<AppointmentResponseDTO> create(
            @PathVariable String tutorCpf,
            @RequestBody @Valid AppointmentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.create(tutorCpf, request));
    }

    @GetMapping("/tutor/{tutorCpf}")
    public ResponseEntity<List<AppointmentResponseDTO>> getTutorAppointments(
            @PathVariable String tutorCpf) {
        return ResponseEntity.ok(appointmentService.getTutorAppointments(tutorCpf));
    }

    @GetMapping("/tutor/{tutorCpf}/veterinarians")
    public ResponseEntity<List<LinkedVeterinarianResponseDTO>> getLinkedVeterinarians(
            @PathVariable String tutorCpf) {
        return ResponseEntity.ok(appointmentService.getLinkedVeterinarians(tutorCpf));
    }

    @GetMapping("/veterinarian/{veterinarianCpf}")
    public ResponseEntity<List<AppointmentResponseDTO>> getVeterinarianAppointments(
            @PathVariable String veterinarianCpf) {
        return ResponseEntity.ok(appointmentService.getVeterinarianAppointments(veterinarianCpf));
    }

    @PatchMapping("/{appointmentId}/confirm")
    public ResponseEntity<AppointmentResponseDTO> confirm(
            @PathVariable Long appointmentId,
            @RequestParam String veterinarianCpf) {
        return ResponseEntity.ok(appointmentService.confirm(appointmentId, veterinarianCpf));
    }

    @PatchMapping("/{appointmentId}/cancel/tutor")
    public ResponseEntity<AppointmentResponseDTO> cancelByTutor(
            @PathVariable Long appointmentId,
            @RequestParam String tutorCpf) {
        return ResponseEntity.ok(appointmentService.cancelByTutor(appointmentId, tutorCpf));
    }

    @PatchMapping("/{appointmentId}/cancel/veterinarian")
    public ResponseEntity<AppointmentResponseDTO> cancelByVeterinarian(
            @PathVariable Long appointmentId,
            @RequestParam String veterinarianCpf) {
        return ResponseEntity.ok(
                appointmentService.cancelByVeterinarian(appointmentId, veterinarianCpf)
        );
    }

}
