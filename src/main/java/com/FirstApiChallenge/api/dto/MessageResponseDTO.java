package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.enums.MessageSenderType;
import com.FirstApiChallenge.api.model.Message;

import java.time.LocalDateTime;

public record MessageResponseDTO(
        Long id,
        Long conversationId,
        MessageSenderType senderType,
        String senderName,
        String content,
        boolean read,
        LocalDateTime sentAt,
        LocalDateTime readAt
) {
    public static MessageResponseDTO fromEntity(Message message) {
        String senderName = message.getSenderType() == MessageSenderType.TUTOR
                ? message.getConversation().getTutor().getName()
                : message.getConversation().getVeterinarian().getName();

        return new MessageResponseDTO(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderType(),
                senderName,
                message.getContent(),
                message.isRead(),
                message.getSentAt(),
                message.getReadAt()
        );
    }
}
