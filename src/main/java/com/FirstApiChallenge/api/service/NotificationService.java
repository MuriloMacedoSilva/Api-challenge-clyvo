package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.NotificationResponseDTO;
import com.FirstApiChallenge.api.enums.DeviceOwnerType;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.event.PushNotificationRequestedEvent;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Notification;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationService implements NotificationPublisher {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationService(
            NotificationRepository notificationRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    // Criar notificação para Tutor COM vínculo associado
    @Override
    @Transactional
    public void createTutorNotification(Tutor tutor, String message, NotificationType type, VeterinarianTutorLink link) {
        Notification notification = new Notification(message, type, tutor, link);
        notificationRepository.save(notification);
        publishPush(DeviceOwnerType.TUTOR, tutor.getId(), message, type, link.getId());
    }

    // Criar notificação para Tutor SEM vínculo
    @Override
    @Transactional
    public void createTutorNotification(Tutor tutor, String message, NotificationType type) {
        Notification notification = new Notification(message, type, tutor);
        notificationRepository.save(notification);
        publishPush(DeviceOwnerType.TUTOR, tutor.getId(), message, type, null);
    }

    // Criar notificação para Veterinário
    @Override
    @Transactional
    public void createVeterinarianNotification(Veterinarian vet, String message, NotificationType type) {
        Notification notification = new Notification(message, type, vet);
        notificationRepository.save(notification);
        publishPush(DeviceOwnerType.VETERINARIAN, vet.getId(), message, type, null);
    }

    private void publishPush(
            DeviceOwnerType recipientType,
            Long recipientId,
            String message,
            NotificationType type,
            Long linkId
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("notificationType", type.name());
        data.put("destination", "notifications");
        if (linkId != null) {
            data.put("linkId", linkId);
        }
        eventPublisher.publishEvent(new PushNotificationRequestedEvent(
                recipientType,
                recipientId,
                titleFor(type),
                message,
                type,
                data
        ));
    }

    private String titleFor(NotificationType type) {
        return switch (type) {
            case LINK_REQUEST_SENT -> "Solicitação de vínculo enviada";
            case LINK_REQUEST_RECEIVED -> "Nova solicitação de vínculo";
            case LINK_REQUEST_ACCEPTED -> "Vínculo aceito";
            case LINK_REQUEST_REJECTED -> "Vínculo recusado";
            case APPOINTMENT_REQUESTED -> "Nova solicitação de consulta";
            case APPOINTMENT_CONFIRMED -> "Consulta confirmada";
            case APPOINTMENT_CANCELLED -> "Consulta cancelada";
            case APPOINTMENT_COMPLETED -> "Consulta concluída";
            case MEDICAL_RECORD_CREATED -> "Novo prontuário";
            case PRESCRIPTION_CREATED -> "Nova prescrição";
            case EXAM_REQUESTED -> "Novo exame";
            case EXAM_RESULT_AVAILABLE -> "Resultado disponível";
            case EXAM_CANCELLED -> "Exame cancelado";
            case VACCINATION_REGISTERED -> "Vacina registrada";
        };
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
