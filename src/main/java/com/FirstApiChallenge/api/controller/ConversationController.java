package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.ChatContactResponseDTO;
import com.FirstApiChallenge.api.dto.ConversationResponseDTO;
import com.FirstApiChallenge.api.dto.MessageRequestDTO;
import com.FirstApiChallenge.api.dto.MessageResponseDTO;
import com.FirstApiChallenge.api.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/v1/conversations")
public class ConversationController {

    private final ChatService chatService;

    public ConversationController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/tutor/{tutorCpf}/veterinarians/{veterinarianCpf}")
    public ResponseEntity<ConversationResponseDTO> getOrCreateForTutor(
            @PathVariable String tutorCpf,
            @PathVariable String veterinarianCpf) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatService.getOrCreateForTutor(tutorCpf, veterinarianCpf));
    }

    @PostMapping("/veterinarian/{veterinarianCpf}/tutors/{tutorCpf}")
    public ResponseEntity<ConversationResponseDTO> getOrCreateForVeterinarian(
            @PathVariable String veterinarianCpf,
            @PathVariable String tutorCpf) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatService.getOrCreateForVeterinarian(veterinarianCpf, tutorCpf));
    }

    @GetMapping("/tutor/{tutorCpf}")
    public ResponseEntity<List<ConversationResponseDTO>> getConversationsForTutor(
            @PathVariable String tutorCpf) {
        return ResponseEntity.ok(chatService.getConversationsForTutor(tutorCpf));
    }

    @GetMapping("/veterinarian/{veterinarianCpf}")
    public ResponseEntity<List<ConversationResponseDTO>> getConversationsForVeterinarian(
            @PathVariable String veterinarianCpf) {
        return ResponseEntity.ok(chatService.getConversationsForVeterinarian(veterinarianCpf));
    }

    @GetMapping("/tutor/{tutorCpf}/contacts")
    public ResponseEntity<List<ChatContactResponseDTO>> getContactsForTutor(
            @PathVariable String tutorCpf) {
        return ResponseEntity.ok(chatService.getContactsForTutor(tutorCpf));
    }

    @GetMapping("/veterinarian/{veterinarianCpf}/contacts")
    public ResponseEntity<List<ChatContactResponseDTO>> getContactsForVeterinarian(
            @PathVariable String veterinarianCpf) {
        return ResponseEntity.ok(chatService.getContactsForVeterinarian(veterinarianCpf));
    }

    @GetMapping("/{conversationId}/messages/tutor/{tutorCpf}")
    public ResponseEntity<List<MessageResponseDTO>> getMessagesForTutor(
            @PathVariable Long conversationId,
            @PathVariable String tutorCpf) {
        return ResponseEntity.ok(chatService.getMessagesForTutor(conversationId, tutorCpf));
    }

    @GetMapping("/{conversationId}/messages/veterinarian/{veterinarianCpf}")
    public ResponseEntity<List<MessageResponseDTO>> getMessagesForVeterinarian(
            @PathVariable Long conversationId,
            @PathVariable String veterinarianCpf) {
        return ResponseEntity.ok(
                chatService.getMessagesForVeterinarian(conversationId, veterinarianCpf)
        );
    }

    @GetMapping("/{conversationId}/tutor/{tutorCpf}")
    public ResponseEntity<ConversationResponseDTO> getConversationForTutor(
            @PathVariable Long conversationId,
            @PathVariable String tutorCpf) {
        return ResponseEntity.ok(chatService.getConversationForTutor(conversationId, tutorCpf));
    }

    @GetMapping("/{conversationId}/veterinarian/{veterinarianCpf}")
    public ResponseEntity<ConversationResponseDTO> getConversationForVeterinarian(
            @PathVariable Long conversationId,
            @PathVariable String veterinarianCpf) {
        return ResponseEntity.ok(
                chatService.getConversationForVeterinarian(conversationId, veterinarianCpf)
        );
    }

    @PostMapping("/{conversationId}/messages/tutor/{tutorCpf}")
    public ResponseEntity<MessageResponseDTO> sendAsTutor(
            @PathVariable Long conversationId,
            @PathVariable String tutorCpf,
            @RequestBody @Valid MessageRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendAsTutor(conversationId, tutorCpf, request));
    }

    @PostMapping("/{conversationId}/messages/veterinarian/{veterinarianCpf}")
    public ResponseEntity<MessageResponseDTO> sendAsVeterinarian(
            @PathVariable Long conversationId,
            @PathVariable String veterinarianCpf,
            @RequestBody @Valid MessageRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendAsVeterinarian(conversationId, veterinarianCpf, request));
    }

    @PatchMapping("/{conversationId}/read/tutor/{tutorCpf}")
    public ResponseEntity<Void> markAsReadForTutor(
            @PathVariable Long conversationId,
            @PathVariable String tutorCpf,
            @RequestParam Long upToMessageId) {
        chatService.markAsReadForTutor(conversationId, tutorCpf, upToMessageId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{conversationId}/read/veterinarian/{veterinarianCpf}")
    public ResponseEntity<Void> markAsReadForVeterinarian(
            @PathVariable Long conversationId,
            @PathVariable String veterinarianCpf,
            @RequestParam Long upToMessageId) {
        chatService.markAsReadForVeterinarian(
                conversationId,
                veterinarianCpf,
                upToMessageId
        );
        return ResponseEntity.noContent().build();
    }
}
