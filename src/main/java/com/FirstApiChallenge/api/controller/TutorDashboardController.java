package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.TutorDashboardResponseDTO;
import com.FirstApiChallenge.api.service.TutorDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboards")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TutorDashboardController {

    private final TutorDashboardService dashboardService;

    public TutorDashboardController(TutorDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/tutor/{tutorCpf}")
    public ResponseEntity<TutorDashboardResponseDTO> getTutorDashboard(
            @PathVariable String tutorCpf) {
        return ResponseEntity.ok(dashboardService.getDashboard(tutorCpf));
    }
}
