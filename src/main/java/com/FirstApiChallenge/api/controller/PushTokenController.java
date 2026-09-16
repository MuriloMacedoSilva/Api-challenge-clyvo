package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.PushTokenRequestDTO;
import com.FirstApiChallenge.api.service.DevicePushTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/push")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Validated
public class PushTokenController {

    private final DevicePushTokenService tokenService;

    public PushTokenController(DevicePushTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/tutor/{tutorCpf}/tokens")
    public ResponseEntity<Void> registerTutorToken(
            @PathVariable String tutorCpf,
            @Valid @RequestBody PushTokenRequestDTO request
    ) {
        tokenService.registerTutorToken(tutorCpf, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/veterinarian/{veterinarianCpf}/tokens")
    public ResponseEntity<Void> registerVeterinarianToken(
            @PathVariable String veterinarianCpf,
            @Valid @RequestBody PushTokenRequestDTO request
    ) {
        tokenService.registerVeterinarianToken(veterinarianCpf, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/tutor/{tutorCpf}/tokens")
    public ResponseEntity<Void> deactivateTutorToken(
            @PathVariable String tutorCpf,
            @RequestParam @NotBlank String token
    ) {
        tokenService.deactivateTutorToken(tutorCpf, token);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/veterinarian/{veterinarianCpf}/tokens")
    public ResponseEntity<Void> deactivateVeterinarianToken(
            @PathVariable String veterinarianCpf,
            @RequestParam @NotBlank String token
    ) {
        tokenService.deactivateVeterinarianToken(veterinarianCpf, token);
        return ResponseEntity.noContent().build();
    }
}
