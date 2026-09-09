package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.NotificationResponseDTO;
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

//    @GetMapping("/tutor/{cpf}")
//    public ResponseEntity<List<Notification>> getTutorNotifications(@PathVariable String cpf) {
//        return ResponseEntity.ok(notificationService.getNotificationsByTutor(cpf));
//    }
//
//    @GetMapping("/veterinarian/{cpf}")
//    public ResponseEntity<List<Notification>> getVetNotifications(@PathVariable String cpf) {
//        return ResponseEntity.ok(notificationService.getNotificationsByVeterinarian(cpf));
//    }

    @GetMapping("/tutor/{cpf}")
    public ResponseEntity<List<NotificationResponseDTO>> getTutorNotifications(
            @PathVariable String cpf) {

        return ResponseEntity.ok(
                notificationService.getNotificationsByTutor(cpf)
        );
    }

    @GetMapping("/veterinarian/{cpf}")
    public ResponseEntity<List<NotificationResponseDTO>> getVetNotifications(
            @PathVariable String cpf) {

        return ResponseEntity.ok(
                notificationService.getNotificationsByVeterinarian(cpf)
        );
    }

    @DeleteMapping("/tutor/{cpf}")
    public ResponseEntity<Void> clearTutorNotifications(@PathVariable String cpf) {
        notificationService.clearTutorNotifications(cpf);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/veterinarian/{cpf}")
    public ResponseEntity<Void> clearVeterinarianNotifications(@PathVariable String cpf) {
        notificationService.clearVeterinarianNotifications(cpf);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}
