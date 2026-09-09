package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.NotificationResponseDTO;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Notification;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Criar notificação para Tutor COM vínculo associado
    @Transactional
    public void createTutorNotification(Tutor tutor, String message, NotificationType type, VeterinarianTutorLink link) {
        Notification notification = new Notification(message, type, tutor, link);
        notificationRepository.save(notification);
    }

    // Criar notificação para Tutor SEM vínculo
    @Transactional
    public void createTutorNotification(Tutor tutor, String message, NotificationType type) {
        Notification notification = new Notification(message, type, tutor);
        notificationRepository.save(notification);
    }

    // Criar notificação para Veterinário
    @Transactional
    public void createVeterinarianNotification(Veterinarian vet, String message, NotificationType type) {
        Notification notification = new Notification(message, type, vet);
        notificationRepository.save(notification);
    }

//    public List<Notification> getNotificationsByTutor(String cpf) {
//        return notificationRepository.findByTutorCpfOrderByCreatedAtDesc(cpf);
//    }
//
//    public List<Notification> getNotificationsByVeterinarian(String cpf) {
//        return notificationRepository.findByVeterinarianCpfOrderByCreatedAtDesc(cpf);
//    }

    public List<NotificationResponseDTO> getNotificationsByTutor(String cpf) {
        return notificationRepository
                .findByTutorCpfOrderByCreatedAtDesc(cpf)
                .stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();
    }

    public List<NotificationResponseDTO> getNotificationsByVeterinarian(String cpf) {
        return notificationRepository
                .findByVeterinarianCpfOrderByCreatedAtDesc(cpf)
                .stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void clearTutorNotifications(String cpf) {
        notificationRepository.deleteByTutorCpf(cpf);
    }

    @Transactional
    public void clearVeterinarianNotifications(String cpf) {
        notificationRepository.deleteByVeterinarianCpf(cpf);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException("Notificação não encontrada", HttpStatus.NOT_FOUND));
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}
