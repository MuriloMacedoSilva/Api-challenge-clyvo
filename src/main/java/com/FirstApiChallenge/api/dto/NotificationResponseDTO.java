package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.model.Notification;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long id,
        String message,
        NotificationType type,
        boolean read,
        LocalDateTime createdAt,
        Long linkId
) {

    public static NotificationResponseDTO fromEntity(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getLink() != null
                        ? notification.getLink().getId()
                        : null
        );
    }
}