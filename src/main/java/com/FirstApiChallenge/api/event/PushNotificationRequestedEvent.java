package com.FirstApiChallenge.api.event;

import com.FirstApiChallenge.api.enums.DeviceOwnerType;
import com.FirstApiChallenge.api.enums.NotificationType;

import java.util.Map;

public record PushNotificationRequestedEvent(
        DeviceOwnerType recipientType,
        Long recipientId,
        String title,
        String body,
        NotificationType notificationType,
        Map<String, Object> data
) {
    public PushNotificationRequestedEvent {
        data = Map.copyOf(data);
    }
}
