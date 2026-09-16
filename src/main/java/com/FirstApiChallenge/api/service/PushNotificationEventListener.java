package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.event.PushNotificationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PushNotificationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushNotificationEventListener.class);

    private final PushNotificationService pushNotificationService;

    public PushNotificationEventListener(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPushRequested(PushNotificationRequestedEvent event) {
        try {
            pushNotificationService.send(event);
        } catch (Exception exception) {
            LOGGER.error("Falha inesperada ao processar push pós-commit: {}", exception.getMessage());
        }
    }
}
