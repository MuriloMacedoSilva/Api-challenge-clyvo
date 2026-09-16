package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.event.PushNotificationRequestedEvent;
import com.FirstApiChallenge.api.model.DevicePushToken;
import com.FirstApiChallenge.api.repository.DevicePushTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PushNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushNotificationService.class);

    private final DevicePushTokenRepository tokenRepository;
    private final ExpoPushClient expoPushClient;

    public PushNotificationService(DevicePushTokenRepository tokenRepository, ExpoPushClient expoPushClient) {
        this.tokenRepository = tokenRepository;
        this.expoPushClient = expoPushClient;
    }

    public void send(PushNotificationRequestedEvent event) {
        List<DevicePushToken> tokens = switch (event.recipientType()) {
            case TUTOR -> tokenRepository.findByTutorIdAndActiveTrue(event.recipientId());
            case VETERINARIAN -> tokenRepository.findByVeterinarianIdAndActiveTrue(event.recipientId());
        };

        for (DevicePushToken token : tokens) {
            sendToToken(token, event);
        }
    }

    private void sendToToken(DevicePushToken token, PushNotificationRequestedEvent event) {
        try {
            ExpoPushTicket ticket = expoPushClient.send(
                    token.getToken(),
                    event.title(),
                    event.body(),
                    event.data()
            );
            if (!ticket.accepted()) {
                LOGGER.warn(
                        "Expo rejeitou token push {}: código={}, mensagem={}",
                        DevicePushTokenService.mask(token.getToken()),
                        ticket.errorCode(),
                        ticket.message()
                );
                if (ticket.deviceNotRegistered()) {
                    deactivateIfStillOwned(token.getToken(), event);
                }
            }
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error(
                    "Falha ao enviar push para token {}: {}",
                    DevicePushTokenService.mask(token.getToken()),
                    exception.getMessage()
            );
        }
    }

    private void deactivateIfStillOwned(String token, PushNotificationRequestedEvent event) {
        switch (event.recipientType()) {
            case TUTOR -> tokenRepository.deactivateActiveTutorToken(token, event.recipientId());
            case VETERINARIAN -> tokenRepository.deactivateActiveVeterinarianToken(token, event.recipientId());
        }
    }
}
