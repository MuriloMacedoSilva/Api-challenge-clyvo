package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.enums.DeviceOwnerType;
import com.FirstApiChallenge.api.enums.DevicePlatform;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.event.PushNotificationRequestedEvent;
import com.FirstApiChallenge.api.model.DevicePushToken;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.repository.DevicePushTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTests {

    @Mock
    private DevicePushTokenRepository tokenRepository;

    @Mock
    private ExpoPushClient expoPushClient;

    @Test
    void sendsOnlyToActiveTokensReturnedForRecipient() throws Exception {
        DevicePushToken token = token("ExponentPushToken[active]");
        when(tokenRepository.findByTutorIdAndActiveTrue(10L)).thenReturn(List.of(token));
        when(expoPushClient.send(any(), any(), any(), any())).thenReturn(ExpoPushTicket.success());

        new PushNotificationService(tokenRepository, expoPushClient).send(event());

        verify(expoPushClient).send(
                eq("ExponentPushToken[active]"),
                eq("Novo exame"),
                eq("Exame solicitado"),
                eq(Map.of("notificationType", "EXAM_REQUESTED"))
        );
    }

    @Test
    void externalFailureDoesNotEscapeOrStopRemainingTokens() throws Exception {
        DevicePushToken first = token("ExponentPushToken[first]");
        DevicePushToken second = token("ExponentPushToken[second]");
        when(tokenRepository.findByTutorIdAndActiveTrue(10L)).thenReturn(List.of(first, second));
        when(expoPushClient.send(eq(first.getToken()), any(), any(), any()))
                .thenThrow(new IllegalStateException("timeout"));
        when(expoPushClient.send(eq(second.getToken()), any(), any(), any()))
                .thenReturn(ExpoPushTicket.success());

        assertDoesNotThrow(() -> new PushNotificationService(tokenRepository, expoPushClient).send(event()));

        verify(expoPushClient).send(eq(second.getToken()), any(), any(), any());
    }

    @Test
    void invalidTokenIsDeactivatedAndDoesNotBlockOthers() throws Exception {
        DevicePushToken invalid = token("ExponentPushToken[invalid]");
        DevicePushToken valid = token("ExponentPushToken[valid]");
        when(tokenRepository.findByTutorIdAndActiveTrue(10L)).thenReturn(List.of(invalid, valid));
        when(expoPushClient.send(eq(invalid.getToken()), any(), any(), any()))
                .thenReturn(ExpoPushTicket.rejected("DeviceNotRegistered", "not registered"));
        when(expoPushClient.send(eq(valid.getToken()), any(), any(), any()))
                .thenReturn(ExpoPushTicket.success());

        new PushNotificationService(tokenRepository, expoPushClient).send(event());

        verify(tokenRepository).deactivateActiveTutorToken(invalid.getToken(), 10L);
        verify(expoPushClient).send(eq(valid.getToken()), any(), any(), any());
    }

    @Test
    void noTokenProducesNoErrorOrExternalCall() throws Exception {
        when(tokenRepository.findByTutorIdAndActiveTrue(10L)).thenReturn(List.of());

        assertDoesNotThrow(() -> new PushNotificationService(tokenRepository, expoPushClient).send(event()));

        verify(expoPushClient, never()).send(any(), any(), any(), any());
    }

    private DevicePushToken token(String value) {
        return new DevicePushToken(value, DevicePlatform.ANDROID, new Tutor());
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
