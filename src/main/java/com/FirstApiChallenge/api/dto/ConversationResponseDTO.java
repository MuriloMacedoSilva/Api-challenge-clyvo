package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.enums.MessageSenderType;
import com.FirstApiChallenge.api.model.Conversation;
import com.FirstApiChallenge.api.model.Message;

import java.time.LocalDateTime;

public record ConversationResponseDTO(
        Long id,
        String tutorCpf,
        String tutorName,
        String veterinarianCpf,
        String veterinarianName,
        String lastMessage,
        LocalDateTime lastMessageAt,
        MessageSenderType lastMessageSenderType,
        long unreadCount,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ConversationResponseDTO fromEntity(
            Conversation conversation,
            Message lastMessage,
            long unreadCount,
            boolean active) {
        return new ConversationResponseDTO(
                conversation.getId(),
                conversation.getTutor().getCpf(),
                conversation.getTutor().getName(),
                conversation.getVeterinarian().getCpf(),
                conversation.getVeterinarian().getName(),
                lastMessage != null ? lastMessage.getContent() : null,
                lastMessage != null ? lastMessage.getSentAt() : null,
                lastMessage != null ? lastMessage.getSenderType() : null,
                unreadCount,
                active,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
