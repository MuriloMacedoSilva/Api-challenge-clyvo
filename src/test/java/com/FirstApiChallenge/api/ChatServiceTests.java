package com.FirstApiChallenge.api;

import com.FirstApiChallenge.api.dto.MessageRequestDTO;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.MessageSenderType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Conversation;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.ConversationRepository;
import com.FirstApiChallenge.api.repository.MessageRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import com.FirstApiChallenge.api.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ChatServiceTests {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private VeterinarianTutorLinkRepository linkRepository;

    private Tutor tutor;
    private Veterinarian veterinarian;
    private VeterinarianTutorLink link;

    @BeforeEach
    void setUp() {
        tutor = tutorRepository.save(createTutor("12345678901", "Tutor A"));
        veterinarian = veterinarianRepository.save(
                createVeterinarian("10987654321", "Vet A", "1234")
        );
        link = addLink(veterinarian, tutor, LinkStatus.ACCEPTED);
    }

    @Test
    void createsConversationWithAcceptedLink() {
        var conversation = openConversation();

        assertNotNull(conversation.id());
        assertEquals(tutor.getCpf(), conversation.tutorCpf());
        assertEquals(veterinarian.getCpf(), conversation.veterinarianCpf());
        assertTrue(conversation.active());
        assertEquals(1, conversationRepository.count());
    }

    @Test
    void reusesExistingConversation() {
        var first = openConversation();
        var second = chatService.getOrCreateForVeterinarian(
                veterinarian.getCpf(),
                tutor.getCpf()
        );

        assertEquals(first.id(), second.id());
        assertEquals(1, conversationRepository.count());
    }

    @Test
    void rejectsConversationWithoutAcceptedLink() {
        Veterinarian pendingVeterinarian = veterinarianRepository.save(
                createVeterinarian("22233344455", "Vet B", "5678")
        );
        addLink(pendingVeterinarian, tutor, LinkStatus.PENDING);

        CustomException exception = assertThrows(CustomException.class, () ->
                chatService.getOrCreateForTutor(tutor.getCpf(), pendingVeterinarian.getCpf())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void tutorSendsUnreadMessage() {
        Long conversationId = openConversation().id();

        var message = chatService.sendAsTutor(
                conversationId,
                tutor.getCpf(),
                new MessageRequestDTO("  A Luna está melhor.  ")
        );

        assertEquals(MessageSenderType.TUTOR, message.senderType());
        assertEquals("A Luna está melhor.", message.content());
        assertFalse(message.read());
        assertNull(message.readAt());
        assertNotNull(message.sentAt());
    }

    @Test
    void veterinarianSendsUnreadMessage() {
        Long conversationId = openConversation().id();

        var message = chatService.sendAsVeterinarian(
                conversationId,
                veterinarian.getCpf(),
                new MessageRequestDTO("Continue observando a alimentação.")
        );

        assertEquals(MessageSenderType.VETERINARIAN, message.senderType());
        assertEquals(veterinarian.getName(), message.senderName());
        assertFalse(message.read());
    }

    @Test
    void rejectsBlankMessage() {
        Long conversationId = openConversation().id();

        CustomException exception = assertThrows(CustomException.class, () -> chatService.sendAsTutor(
                conversationId,
                tutor.getCpf(),
                new MessageRequestDTO("   ")
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void wrongTutorCannotAccessConversation() {
        Long conversationId = openConversation().id();
        Tutor otherTutor = tutorRepository.save(createTutor("22233344455", "Tutor B"));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> chatService.getMessagesForTutor(conversationId, otherTutor.getCpf())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void wrongVeterinarianCannotAccessConversation() {
        Long conversationId = openConversation().id();
        Veterinarian otherVeterinarian = veterinarianRepository.save(
                createVeterinarian("22233344455", "Vet B", "5678")
        );

        CustomException exception = assertThrows(
                CustomException.class,
                () -> chatService.getMessagesForVeterinarian(
                        conversationId,
                        otherVeterinarian.getCpf()
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void returnsMessagesOrderedBySentAtAndId() {
        Long conversationId = openConversation().id();
        chatService.sendAsTutor(conversationId, tutor.getCpf(), new MessageRequestDTO("Mensagem 1"));
        chatService.sendAsVeterinarian(
                conversationId,
                veterinarian.getCpf(),
                new MessageRequestDTO("Mensagem 2")
        );
        chatService.sendAsTutor(conversationId, tutor.getCpf(), new MessageRequestDTO("Mensagem 3"));

        var messages = chatService.getMessagesForTutor(conversationId, tutor.getCpf());

        assertEquals(3, messages.size());
        assertEquals("Mensagem 1", messages.get(0).content());
        assertEquals("Mensagem 2", messages.get(1).content());
        assertEquals("Mensagem 3", messages.get(2).content());
    }

    @Test
    void tutorMarksOnlyVeterinarianMessagesAsRead() {
        Long conversationId = openConversation().id();
        chatService.sendAsVeterinarian(
                conversationId,
                veterinarian.getCpf(),
                new MessageRequestDTO("Mensagem do Vet 1")
        );
        chatService.sendAsVeterinarian(
                conversationId,
                veterinarian.getCpf(),
                new MessageRequestDTO("Mensagem do Vet 2")
        );
        chatService.sendAsTutor(
                conversationId,
                tutor.getCpf(),
                new MessageRequestDTO("Mensagem do Tutor")
        );

        Long upToMessageId = messageRepository
                .findTopByConversationIdOrderBySentAtDescIdDesc(conversationId)
                .orElseThrow()
                .getId();
        assertEquals(2, chatService.markAsReadForTutor(conversationId, tutor.getCpf(), upToMessageId));
        var messages = messageRepository.findByConversationIdOrderBySentAtAscIdAsc(conversationId);
        assertTrue(messages.get(0).isRead());
        assertTrue(messages.get(1).isRead());
        assertFalse(messages.get(2).isRead());
        assertNotNull(messages.get(0).getReadAt());
    }

    @Test
    void veterinarianMarksOnlyTutorMessagesAsRead() {
        Long conversationId = openConversation().id();
        chatService.sendAsTutor(
                conversationId,
                tutor.getCpf(),
                new MessageRequestDTO("Mensagem do Tutor 1")
        );
        chatService.sendAsTutor(
                conversationId,
                tutor.getCpf(),
                new MessageRequestDTO("Mensagem do Tutor 2")
        );
        chatService.sendAsVeterinarian(
                conversationId,
                veterinarian.getCpf(),
                new MessageRequestDTO("Mensagem do Vet")
        );

        assertEquals(
                2,
                chatService.markAsReadForVeterinarian(
                        conversationId,
                        veterinarian.getCpf(),
                        messageRepository.findTopByConversationIdOrderBySentAtDescIdDesc(conversationId)
                                .orElseThrow()
                                .getId()
                )
        );
        var messages = messageRepository.findByConversationIdOrderBySentAtAscIdAsc(conversationId);
        assertTrue(messages.get(0).isRead());
        assertTrue(messages.get(1).isRead());
        assertFalse(messages.get(2).isRead());
    }

    @Test
    void conversationListCountsOnlyIncomingUnreadMessages() {
        Long conversationId = openConversation().id();
        for (int index = 1; index <= 3; index++) {
            chatService.sendAsVeterinarian(
                    conversationId,
                    veterinarian.getCpf(),
                    new MessageRequestDTO("Mensagem do Vet " + index)
            );
        }
        chatService.sendAsTutor(conversationId, tutor.getCpf(), new MessageRequestDTO("Resposta 1"));
        chatService.sendAsTutor(conversationId, tutor.getCpf(), new MessageRequestDTO("Resposta 2"));

        var tutorConversation = chatService.getConversationsForTutor(tutor.getCpf()).get(0);
        var veterinarianConversation = chatService
                .getConversationsForVeterinarian(veterinarian.getCpf())
                .get(0);

        assertEquals(3, tutorConversation.unreadCount());
        assertEquals(2, veterinarianConversation.unreadCount());
        assertEquals("Resposta 2", tutorConversation.lastMessage());
    }

    @Test
    void inactiveLinkKeepsHistoryButBlocksNewMessages() {
        Long conversationId = openConversation().id();
        chatService.sendAsTutor(
                conversationId,
                tutor.getCpf(),
                new MessageRequestDTO("Mensagem preservada")
        );
        link.setStatus(LinkStatus.REJECTED);
        linkRepository.saveAndFlush(link);

        var history = chatService.getMessagesForVeterinarian(
                conversationId,
                veterinarian.getCpf()
        );
        CustomException exception = assertThrows(CustomException.class, () ->
                chatService.sendAsVeterinarian(
                        conversationId,
                        veterinarian.getCpf(),
                        new MessageRequestDTO("Nova mensagem")
                )
        );

        assertEquals(1, history.size());
        assertEquals("Mensagem preservada", history.get(0).content());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertFalse(chatService.getConversationsForTutor(tutor.getCpf()).get(0).active());
    }

    @Test
    void userWithoutConversationsReceivesEmptyList() {
        Tutor otherTutor = tutorRepository.save(createTutor("22233344455", "Tutor B"));
        Veterinarian otherVeterinarian = veterinarianRepository.save(
                createVeterinarian("33344455566", "Vet B", "5678")
        );

        assertTrue(chatService.getConversationsForTutor(otherTutor.getCpf()).isEmpty());
        assertTrue(chatService.getConversationsForVeterinarian(otherVeterinarian.getCpf()).isEmpty());
    }

    @Test
    void databaseRejectsDuplicateConversationPair() {
        openConversation();
        Conversation duplicate = new Conversation();
        duplicate.setTutor(tutor);
        duplicate.setVeterinarian(veterinarian);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> conversationRepository.saveAndFlush(duplicate)
        );
    }

    private com.FirstApiChallenge.api.dto.ConversationResponseDTO openConversation() {
        return chatService.getOrCreateForTutor(tutor.getCpf(), veterinarian.getCpf());
    }

    private VeterinarianTutorLink addLink(
            Veterinarian linkedVeterinarian,
            Tutor linkedTutor,
            LinkStatus status) {
        VeterinarianTutorLink value = new VeterinarianTutorLink(linkedVeterinarian, linkedTutor);
        value.setStatus(status);
        return linkRepository.save(value);
    }

    private Tutor createTutor(String cpf, String name) {
        Tutor value = new Tutor();
        value.setName(name);
        value.setEmail(cpf + "@email.com");
        value.setCpf(cpf);
        value.setPhoneNumber("11999999999");
        value.setPassword("12345678");
        value.setRole("tutor");
        return value;
    }

    private Veterinarian createVeterinarian(String cpf, String name, String crmv) {
        Veterinarian value = new Veterinarian();
        value.setName(name);
        value.setEmail(cpf + "@email.com");
        value.setCpf(cpf);
        value.setPhoneNumber("11999999999");
        value.setPassword("12345678");
        value.setRole("veterinarian");
        value.setCrmvNumber(crmv);
        value.setCrmvState("SP");
        value.setCnpj("12345678000199");
        return value;
    }
}
