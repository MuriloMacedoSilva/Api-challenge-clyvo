package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.AnimalResponseDTO;
import com.FirstApiChallenge.api.dto.LinkResponseDTO;
import com.FirstApiChallenge.api.dto.TutorResponseDTO;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LinkService {

    private final VeterinarianTutorLinkRepository linkRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final NotificationService notificationService;

    public LinkService(VeterinarianTutorLinkRepository linkRepository,
                       TutorRepository tutorRepository,
                       VeterinarianRepository veterinarianRepository,
                       NotificationService notificationService) {
        this.linkRepository = linkRepository;
        this.tutorRepository = tutorRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.notificationService = notificationService;
    }

    // 1. Veterinário busca tutor pelo CPF e envia a solicitação
    @Transactional
    public void sendLinkRequest(String crmvNumber, String tutorCpf) {
        Veterinarian veterinarian = veterinarianRepository.findByCrmvNumber(crmvNumber)
                .orElseThrow(() -> new CustomException("Veterinário não encontrado", HttpStatus.NOT_FOUND));

        Tutor tutor = tutorRepository.findByCpf(tutorCpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado para o CPF informado", HttpStatus.NOT_FOUND));

        Optional<VeterinarianTutorLink> existingLink =
                linkRepository.findByVeterinarianCrmvNumberAndTutorCpf(crmvNumber, tutorCpf);

        VeterinarianTutorLink link;

        if (existingLink.isPresent()) {
            link = existingLink.get();

            if (link.getStatus() == LinkStatus.ACCEPTED) {
                throw new CustomException("Você já possui vínculo ativo com este tutor", HttpStatus.CONFLICT);
            }
            if (link.getStatus() == LinkStatus.PENDING) {
                throw new CustomException("Já existe uma solicitação pendente para este tutor", HttpStatus.CONFLICT);
            }

            link.setStatus(LinkStatus.PENDING);
            link.setUpdatedAt(LocalDateTime.now());
        } else {
            link = new VeterinarianTutorLink(veterinarian, tutor);
        }

        VeterinarianTutorLink savedLink = linkRepository.save(link);

        // --- DISPARO DE NOTIFICAÇÕES ---
        // Notificação para o Veterinário
        notificationService.createVeterinarianNotification(
                veterinarian,
                "Solicitação de vínculo enviada para o tutor " + tutor.getName() + ". Status: não respondida",
                NotificationType.LINK_REQUEST_SENT
        );

        // Notificação para o Tutor (passando o savedLink com o ID gerado)
        notificationService.createTutorNotification(
                tutor,
                "Você recebeu uma solicitação de vínculo do veterinário Dr(a). " + veterinarian.getName(),
                NotificationType.LINK_REQUEST_RECEIVED,
                savedLink
        );
    }

    // 2. Tutor responde à solicitação (ACEITAR ou RECUSAR)
    @Transactional
    public void respondToLinkRequest(Long linkId, String tutorCpf, boolean accept) {
        VeterinarianTutorLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new CustomException("Solicitação não encontrada", HttpStatus.NOT_FOUND));

        if (!link.getTutor().getCpf().equals(tutorCpf)) {
            throw new CustomException("Você não tem permissão para responder a esta solicitação", HttpStatus.FORBIDDEN);
        }

        if (link.getStatus() != LinkStatus.PENDING) {
            throw new CustomException("Esta solicitação já foi respondida anteriormente", HttpStatus.BAD_REQUEST);
        }

        link.setStatus(accept ? LinkStatus.ACCEPTED : LinkStatus.REJECTED);
        link.setUpdatedAt(LocalDateTime.now());
        linkRepository.save(link);

        // --- DISPARO DE NOTIFICAÇÕES DE RESPOSTA ---
        if (accept) {
            notificationService.createVeterinarianNotification(
                    link.getVeterinarian(),
                    "O tutor " + link.getTutor().getName() + " aceitou sua solicitação de vínculo!",
                    NotificationType.LINK_REQUEST_ACCEPTED
            );
        } else {
            notificationService.createVeterinarianNotification(
                    link.getVeterinarian(),
                    "O tutor " + link.getTutor().getName() + " recusou sua solicitação de vínculo.",
                    NotificationType.LINK_REQUEST_REJECTED
            );
        }
    }

//    public List<VeterinarianTutorLink> getPendingRequestsForTutor(String tutorCpf) {
//        return linkRepository.findByTutorCpfAndStatus(tutorCpf, LinkStatus.PENDING);
//    }

    @Transactional(readOnly = true)
    public List<LinkResponseDTO> getPendingRequestsForTutor(String tutorCpf) {

        return linkRepository
                .findByTutorCpfAndStatus(
                        tutorCpf,
                        LinkStatus.PENDING
                )
                .stream()
                .map(LinkResponseDTO::fromEntity)
                .toList();
    }

//    public Set<Animal> getTutorAnimalsIfLinked(String veterinarianCpf, String tutorCpf) {
//        boolean isLinked = linkRepository.existsByVeterinarianCpfAndTutorCpfAndStatus(veterinarianCpf, tutorCpf, LinkStatus.ACCEPTED);
//
//        if (!isLinked) {
//            throw new CustomException("Acesso negado: Você não possui vínculo ativo com este tutor", HttpStatus.FORBIDDEN);
//        }
//
//        Tutor tutor = tutorRepository.findByCpf(tutorCpf)
//                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));
//
//        return tutor.getAnimals();
//    }

    @Transactional(readOnly = true)
    public Set<AnimalResponseDTO> getTutorAnimalsIfLinked(
            String veterinarianCpf,
            String tutorCpf) {

        boolean isLinked =
                linkRepository
                        .existsByVeterinarianCpfAndTutorCpfAndStatus(
                                veterinarianCpf,
                                tutorCpf,
                                LinkStatus.ACCEPTED
                        );

        if (!isLinked) {
            throw new CustomException(
                    "Acesso negado: Você não possui vínculo ativo com este tutor",
                    HttpStatus.FORBIDDEN
            );
        }

        Tutor tutor = tutorRepository.findByCpf(tutorCpf)
                .orElseThrow(() ->
                        new CustomException(
                                "Tutor não encontrado",
                                HttpStatus.NOT_FOUND
                        )
                );

        return tutor.getAnimals()
                .stream()
                .map(AnimalResponseDTO::fromEntity)
                .collect(Collectors.toSet());
    }

    public List<TutorResponseDTO> getLinkedTutorsForVet(String veterinarianCpf) {
        List<VeterinarianTutorLink> links = linkRepository.findByVeterinarianCpfAndStatus(veterinarianCpf, LinkStatus.ACCEPTED);

        return links.stream()
                .map(link -> TutorResponseDTO.fromEntity(link.getTutor()))
                .collect(Collectors.toList());
    }
}
