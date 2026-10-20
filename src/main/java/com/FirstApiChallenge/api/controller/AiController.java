package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.AiAnswerResponseDTO;
import com.FirstApiChallenge.api.dto.AiQuestionRequestDTO;
import com.FirstApiChallenge.api.service.AiOrchestrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ai")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AiController {

    private final AiOrchestrationService aiOrchestrationService;

    public AiController(AiOrchestrationService aiOrchestrationService) {
        this.aiOrchestrationService = aiOrchestrationService;
    }

    @PostMapping("/tutor/{tutorCpf}/animals/{animalId}")
    public ResponseEntity<AiAnswerResponseDTO> askAsTutor(
            @PathVariable String tutorCpf,
            @PathVariable Long animalId,
            @Valid @RequestBody AiQuestionRequestDTO request) {
        return ResponseEntity.ok(aiOrchestrationService.askTutor(tutorCpf, animalId, request.question()));
    }
}
