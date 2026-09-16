package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.enums.DeviceOwnerType;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.event.PushNotificationRequestedEvent;
import com.FirstApiChallenge.api.model.Notification;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationPushPublishingTests {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void persistsInternalNotificationBeforeRequestingPush() {
        Tutor tutor = new Tutor();
        tutor.setId(42L);
        NotificationService service = new NotificationService(notificationRepository, eventPublisher);

        service.createTutorNotification(
                tutor,
                "Uma nova vacinação foi registrada para Luna.",
                NotificationType.VACCINATION_REGISTERED
        );

        InOrder order = inOrder(notificationRepository, eventPublisher);
        order.verify(notificationRepository).save(any(Notification.class));
        order.verify(eventPublisher).publishEvent(any(PushNotificationRequestedEvent.class));

        ArgumentCaptor<PushNotificationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(PushNotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PushNotificationRequestedEvent event = eventCaptor.getValue();
        assertEquals(DeviceOwnerType.TUTOR, event.recipientType());
        assertEquals(42L, event.recipientId());
        assertEquals("Vacina registrada", event.title());
        assertEquals("VACCINATION_REGISTERED", event.data().get("notificationType"));
    }

    @Test
    void listenerRunsOnlyAfterCommit() throws Exception {
        Method method = PushNotificationEventListener.class.getMethod(
                "onPushRequested",
                PushNotificationRequestedEvent.class
        );
        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);

        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
    }
}
