package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.model.Notification;
import com.FirstApiChallenge.api.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/tutor/{cpf}")
    public ResponseEntity<List<Notification>> getTutorNotifications(@PathVariable String cpf) {
        return ResponseEntity.ok(notificationService.getNotificationsByTutor(cpf));
    }

    @GetMapping("/veterinarian/{crmvNumber}")
    public ResponseEntity<List<Notification>> getVetNotifications(@PathVariable String crmvNumber) {
        return ResponseEntity.ok(notificationService.getNotificationsByVeterinarian(crmvNumber));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}