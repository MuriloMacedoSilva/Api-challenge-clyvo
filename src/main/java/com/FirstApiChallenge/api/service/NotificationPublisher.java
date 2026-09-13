package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;

public interface NotificationPublisher {

    void createTutorNotification(
            Tutor tutor,
            String message,
            NotificationType type,
            VeterinarianTutorLink link);

    void createTutorNotification(Tutor tutor, String message, NotificationType type);

    void createVeterinarianNotification(
            Veterinarian veterinarian,
            String message,
            NotificationType type);
}
