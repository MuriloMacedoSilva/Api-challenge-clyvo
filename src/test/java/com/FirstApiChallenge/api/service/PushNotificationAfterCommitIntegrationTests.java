package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.enums.DeviceOwnerType;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.event.PushNotificationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
class PushNotificationAfterCommitIntegrationTests {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private PushNotificationService pushNotificationService;

    @BeforeEach
    void resetMock() {
        reset(pushNotificationService);
    }

    @Test
    void deliversPushEventAfterCommit() {
        PushNotificationRequestedEvent event = event();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        verify(pushNotificationService).send(event);
    }

    @Test
    void doesNotDeliverPushEventAfterRollback() {
        PushNotificationRequestedEvent event = event();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            status.setRollbackOnly();
        });

        verify(pushNotificationService, never()).send(event);
    }

    private PushNotificationRequestedEvent event() {
        return new PushNotificationRequestedEvent(
                DeviceOwnerType.TUTOR,
                10L,
                "Novo exame",
                "Exame solicitado",
                NotificationType.EXAM_REQUESTED,
                Map.of("notificationType", "EXAM_REQUESTED")
        );
    }
}
