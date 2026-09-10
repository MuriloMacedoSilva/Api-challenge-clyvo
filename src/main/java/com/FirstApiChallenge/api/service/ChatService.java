package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.ChatContactResponseDTO;
import com.FirstApiChallenge.api.dto.ConversationResponseDTO;
import com.FirstApiChallenge.api.dto.MessageRequestDTO;
import com.FirstApiChallenge.api.dto.MessageResponseDTO;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.MessageSenderType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Conversation;
import com.FirstApiChallenge.api.model.Message;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.ConversationRepository;
import com.FirstApiChallenge.api.repository.MessageRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final VeterinarianTutorLinkRepository linkRepository;

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            TutorRepository tutorRepository,
            VeterinarianRepository veterinarianRepository,
            VeterinarianTutorLinkRepository linkRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.tutorRepository = tutorRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.linkRepository = linkRepository;
    }

    @Transactional
    public ConversationResponseDTO getOrCreateForTutor(String tutorCpf, String veterinarianCpf) {
        return getOrCreate(tutorCpf, veterinarianCpf, MessageSenderType.TUTOR);
    }

    @Transactional
    public ConversationResponseDTO getOrCreateForVeterinarian(
            String veterinarianCpf,
            String tutorCpf) {
        return getOrCreate(tutorCpf, veterinarianCpf, MessageSenderType.VETERINARIAN);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponseDTO> getConversationsForTutor(String tutorCpf) {
        findTutor(tutorCpf);
        List<Conversation> conversations = conversationRepository
                .findByTutorCpfOrderByUpdatedAtDesc(tutorCpf);
        Set<String> activeVeterinarians = linkRepository
                .findByTutorCpfAndStatus(tutorCpf, LinkStatus.ACCEPTED)
                .stream()
                .map(link -> link.getVeterinarian().getCpf())
                .collect(Collectors.toSet());

        return mapConversationList(
                conversations,
                MessageSenderType.VETERINARIAN,
                conversation -> activeVeterinarians.contains(conversation.getVeterinarian().getCpf())
        );
    }

    @Transactional(readOnly = true)
    public List<ConversationResponseDTO> getConversationsForVeterinarian(String veterinarianCpf) {
        findVeterinarian(veterinarianCpf);
        List<Conversation> conversations = conversationRepository
                .findByVeterinarianCpfOrderByUpdatedAtDesc(veterinarianCpf);
        Set<String> activeTutors = linkRepository
                .findByVeterinarianCpfAndStatus(veterinarianCpf, LinkStatus.ACCEPTED)
                .stream()
                .map(link -> link.getTutor().getCpf())
                .collect(Collectors.toSet());

        return mapConversationList(
                conversations,
                MessageSenderType.TUTOR,
                conversation -> activeTutors.contains(conversation.getTutor().getCpf())
        );
    }

    @Transactional(readOnly = true)
    public List<ChatContactResponseDTO> getContactsForTutor(String tutorCpf) {
        findTutor(tutorCpf);
        return linkRepository.findByTutorCpfAndStatus(tutorCpf, LinkStatus.ACCEPTED)
                .stream()
                .map(VeterinarianTutorLink::getVeterinarian)
                .map(ChatContactResponseDTO::fromVeterinarian)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatContactResponseDTO> getContactsForVeterinarian(String veterinarianCpf) {
        findVeterinarian(veterinarianCpf);
        return linkRepository.findByVeterinarianCpfAndStatus(veterinarianCpf, LinkStatus.ACCEPTED)
                .stream()
                .map(VeterinarianTutorLink::getTutor)
                .map(ChatContactResponseDTO::fromTutor)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getMessagesForTutor(Long conversationId, String tutorCpf) {
        Tutor tutor = findTutor(tutorCpf);
        Conversation conversation = findConversation(conversationId);
        validateTutorParticipant(conversation, tutor);
        return getMessages(conversationId);
    }

    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getMessagesForVeterinarian(
            Long conversationId,
            String veterinarianCpf) {
        Veterinarian veterinarian = findVeterinarian(veterinarianCpf);
        Conversation conversation = findConversation(conversationId);
        validateVeterinarianParticipant(conversation, veterinarian);
        return getMessages(conversationId);
    }

    @Transactional(readOnly = true)
    public ConversationResponseDTO getConversationForTutor(Long conversationId, String tutorCpf) {
        Tutor tutor = findTutor(tutorCpf);
        Conversation conversation = findConversation(conversationId);
        validateTutorParticipant(conversation, tutor);
        return getConversationResponse(
                conversation,
                MessageSenderType.VETERINARIAN,
                hasAcceptedLink(conversation)
        );
    }

    @Transactional(readOnly = true)
    public ConversationResponseDTO getConversationForVeterinarian(
            Long conversationId,
            String veterinarianCpf) {
        Veterinarian veterinarian = findVeterinarian(veterinarianCpf);
        Conversation conversation = findConversation(conversationId);
        validateVeterinarianParticipant(conversation, veterinarian);
        return getConversationResponse(
                conversation,
                MessageSenderType.TUTOR,
                hasAcceptedLink(conversation)
        );
    }

    @Transactional
    public MessageResponseDTO sendAsTutor(
            Long conversationId,
            String tutorCpf,
            MessageRequestDTO request) {
        Tutor tutor = findTutor(tutorCpf);
        Conversation conversation = findConversation(conversationId);
        validateTutorParticipant(conversation, tutor);
        validateAcceptedLinkForUpdate(conversation);
        return sendMessage(conversation, MessageSenderType.TUTOR, request);
    }

    @Transactional
    public MessageResponseDTO sendAsVeterinarian(
            Long conversationId,
            String veterinarianCpf,
            MessageRequestDTO request) {
        Veterinarian veterinarian = findVeterinarian(veterinarianCpf);
        Conversation conversation = findConversation(conversationId);
        validateVeterinarianParticipant(conversation, veterinarian);
        validateAcceptedLinkForUpdate(conversation);
        return sendMessage(conversation, MessageSenderType.VETERINARIAN, request);
    }

    @Transactional
    public int markAsReadForTutor(Long conversationId, String tutorCpf, Long upToMessageId) {
        Tutor tutor = findTutor(tutorCpf);
        Conversation conversation = findConversation(conversationId);
        validateTutorParticipant(conversation, tutor);
        return messageRepository.markUnreadAsRead(
                conversationId,
                MessageSenderType.VETERINARIAN,
                upToMessageId,
                LocalDateTime.now()
        );
    }

    @Transactional
    public int markAsReadForVeterinarian(
            Long conversationId,
            String veterinarianCpf,
            Long upToMessageId) {
        Veterinarian veterinarian = findVeterinarian(veterinarianCpf);
        Conversation conversation = findConversation(conversationId);
        validateVeterinarianParticipant(conversation, veterinarian);
        return messageRepository.markUnreadAsRead(
                conversationId,
                MessageSenderType.TUTOR,
                upToMessageId,
                LocalDateTime.now()
        );
    }

    private ConversationResponseDTO getOrCreate(
            String tutorCpf,
            String veterinarianCpf,
            MessageSenderType viewerType) {
        Tutor tutor = findTutor(tutorCpf);
        Veterinarian veterinarian = findVeterinarian(veterinarianCpf);

        linkRepository.findByParticipantsAndStatusForUpdate(
                veterinarianCpf,
                tutorCpf,
                LinkStatus.ACCEPTED
        ).orElseThrow(() -> new CustomException(
                "Tutor e veterinário não possuem vínculo aceito",
                HttpStatus.FORBIDDEN
        ));

        Conversation conversation = conversationRepository
                .findByTutorIdAndVeterinarianId(tutor.getId(), veterinarian.getId())
                .orElseGet(() -> {
                    Conversation value = new Conversation();
                    value.setTutor(tutor);
                    value.setVeterinarian(veterinarian);
                    return conversationRepository.saveAndFlush(value);
                });

        MessageSenderType incomingType = viewerType == MessageSenderType.TUTOR
                ? MessageSenderType.VETERINARIAN
                : MessageSenderType.TUTOR;
        Message lastMessage = messageRepository
                .findTopByConversationIdOrderBySentAtDescIdDesc(conversation.getId())
                .orElse(null);
        long unreadCount = messageRepository.countByConversationIdAndSenderTypeAndReadFalse(
                conversation.getId(),
                incomingType
        );

        return ConversationResponseDTO.fromEntity(conversation, lastMessage, unreadCount, true);
    }

    private List<ConversationResponseDTO> mapConversationList(
            List<Conversation> conversations,
            MessageSenderType incomingType,
            java.util.function.Predicate<Conversation> activeCheck) {
        if (conversations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> conversationIds = conversations.stream().map(Conversation::getId).toList();
        Map<Long, Message> lastMessages = messageRepository
                .findLatestByConversationIds(conversationIds)
                .stream()
                .collect(Collectors.toMap(message -> message.getConversation().getId(), Function.identity()));
        Map<Long, Long> unreadCounts = messageRepository
                .countUnreadByConversationIds(conversationIds, incomingType)
                .stream()
                .collect(Collectors.toMap(
                        MessageRepository.UnreadCountProjection::getConversationId,
                        MessageRepository.UnreadCountProjection::getUnreadCount
                ));

        return conversations.stream()
                .map(conversation -> ConversationResponseDTO.fromEntity(
                        conversation,
                        lastMessages.get(conversation.getId()),
                        unreadCounts.getOrDefault(conversation.getId(), 0L),
                        activeCheck.test(conversation)
                ))
                .toList();
    }

    private List<MessageResponseDTO> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderBySentAtAscIdAsc(conversationId)
                .stream()
                .map(MessageResponseDTO::fromEntity)
                .toList();
    }

    private ConversationResponseDTO getConversationResponse(
            Conversation conversation,
            MessageSenderType incomingType,
            boolean active) {
        Message lastMessage = messageRepository
                .findTopByConversationIdOrderBySentAtDescIdDesc(conversation.getId())
                .orElse(null);
        long unreadCount = messageRepository.countByConversationIdAndSenderTypeAndReadFalse(
                conversation.getId(),
                incomingType
        );
        return ConversationResponseDTO.fromEntity(conversation, lastMessage, unreadCount, active);
    }

    private MessageResponseDTO sendMessage(
            Conversation conversation,
            MessageSenderType senderType,
            MessageRequestDTO request) {
        validateMessage(request);
        LocalDateTime now = LocalDateTime.now();

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderType(senderType);
        message.setContent(request.content().trim());
        message.setRead(false);
        message.setSentAt(now);

        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);
        return MessageResponseDTO.fromEntity(messageRepository.saveAndFlush(message));
    }

    private void validateMessage(MessageRequestDTO request) {
        if (request.content() == null || request.content().isBlank()) {
            throw new CustomException("Conteúdo da mensagem é obrigatório", HttpStatus.BAD_REQUEST);
        }
        if (request.content().trim().length() > 2000) {
            throw new CustomException(
                    "Mensagem deve ter no máximo 2000 caracteres",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private boolean hasAcceptedLink(Conversation conversation) {
        return linkRepository.existsByVeterinarianCpfAndTutorCpfAndStatus(
                conversation.getVeterinarian().getCpf(),
                conversation.getTutor().getCpf(),
                LinkStatus.ACCEPTED
        );
    }

    private void validateAcceptedLinkForUpdate(Conversation conversation) {
        linkRepository.findByParticipantsAndStatusForUpdate(
                conversation.getVeterinarian().getCpf(),
                conversation.getTutor().getCpf(),
                LinkStatus.ACCEPTED
        ).orElseThrow(() -> new CustomException(
                "A conversa está indisponível porque o vínculo não está ativo",
                HttpStatus.FORBIDDEN
        ));
    }

    private void validateTutorParticipant(Conversation conversation, Tutor tutor) {
        if (!conversation.getTutor().getId().equals(tutor.getId())) {
            throw new CustomException("Esta conversa não pertence ao tutor informado", HttpStatus.FORBIDDEN);
        }
    }

    private void validateVeterinarianParticipant(
            Conversation conversation,
            Veterinarian veterinarian) {
        if (!conversation.getVeterinarian().getId().equals(veterinarian.getId())) {
            throw new CustomException(
                    "Esta conversa não pertence ao veterinário informado",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private Conversation findConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new CustomException("Conversa não encontrada", HttpStatus.NOT_FOUND));
    }

    private Tutor findTutor(String tutorCpf) {
        return tutorRepository.findByCpf(tutorCpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));
    }

    private Veterinarian findVeterinarian(String veterinarianCpf) {
        return veterinarianRepository.findByCpf(veterinarianCpf)
                .orElseThrow(() -> new CustomException("Veterinário não encontrado", HttpStatus.NOT_FOUND));
    }
}
