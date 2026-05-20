package com.FirstApiChallenge.api.controler;

import com.FirstApiChallenge.api.dto.*;
import com.FirstApiChallenge.api.service.VeterinarianService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/veterinarian")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class VeterinarianController {

    private final VeterinarianService veterinarianService;

    public VeterinarianController(VeterinarianService veterinarianService) {
        this.veterinarianService = veterinarianService;
    }

    @PostMapping
    public ResponseEntity<VeterinarianResponseDTO> create(@RequestBody @Valid VeterinarianRequestDTO dto){
        VeterinarianResponseDTO response = veterinarianService.createVeterinarian(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/Login")
    public ResponseEntity<?> login(@RequestBody VeterinarianRequestDTO dto) {
        VeterinarianResponseDTO response;
        try {
            response = veterinarianService.authenticateVeterinarian(dto);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<VeterinarianResponseDTO> searchVeterinarianByCpf(@PathVariable String cpf){
        return veterinarianService.searchVeterinarianByCpf(cpf)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

