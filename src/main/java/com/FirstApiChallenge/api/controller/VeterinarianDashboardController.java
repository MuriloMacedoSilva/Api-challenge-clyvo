package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.VeterinarianDashboardResponseDTO;
import com.FirstApiChallenge.api.service.VeterinarianDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboards")
public class VeterinarianDashboardController {

    private final VeterinarianDashboardService dashboardService;

    public VeterinarianDashboardController(VeterinarianDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/veterinarian/{veterinarianCpf}")
    public ResponseEntity<VeterinarianDashboardResponseDTO> getVeterinarianDashboard(
            @PathVariable String veterinarianCpf) {
        return ResponseEntity.ok(dashboardService.getDashboard(veterinarianCpf));
    }
}
