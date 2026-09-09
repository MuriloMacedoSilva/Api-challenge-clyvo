package com.FirstApiChallenge.api.controller;

import com.FirstApiChallenge.api.dto.AnimalResponseDTO;
import com.FirstApiChallenge.api.dto.LinkResponseDTO;
import com.FirstApiChallenge.api.dto.TutorResponseDTO;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.service.LinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/links")
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    // Veterinário envia solicitação para o tutor
    @PostMapping("/request")
    public ResponseEntity<Void> requestLink(@RequestParam String crmvNumber, @RequestParam String tutorCpf) {
        linkService.sendLinkRequest(crmvNumber, tutorCpf);
        return ResponseEntity.ok().build();
    }

    // Tutor aceita ou recusa solicitação
    @PatchMapping("/{linkId}/respond")
    public ResponseEntity<Void> respondLink(
            @PathVariable Long linkId,
            @RequestParam String tutorCpf,
            @RequestParam boolean accept) {
        linkService.respondToLinkRequest(linkId, tutorCpf, accept);
        return ResponseEntity.noContent().build();
    }

    // Tutor visualiza pedidos pendentes
//    @GetMapping("/pending")
//    public ResponseEntity<List<VeterinarianTutorLink>> getPendingRequests(@RequestParam String tutorCpf) {
//        List<VeterinarianTutorLink> pendingList = linkService.getPendingRequestsForTutor(tutorCpf);
//        return ResponseEntity.ok(pendingList);
//    }

    @GetMapping("/pending")
    public ResponseEntity<List<LinkResponseDTO>> getPendingRequests(
            @RequestParam String tutorCpf) {

        List<LinkResponseDTO> pendingList =
                linkService.getPendingRequestsForTutor(tutorCpf);

        return ResponseEntity.ok(pendingList);
    }

//    // Veterinário consulta os pets do tutor (valida se o status é ACCEPTED)
//    @GetMapping("/veterinarian/{veterinarianCpf}/tutor/{tutorCpf}/animals")
//    public ResponseEntity<Set<Animal>> getAnimalsForLinkedVet(
//            @PathVariable String veterinarianCpf,
//            @PathVariable String tutorCpf) {
//        Set<Animal> animals = linkService.getTutorAnimalsIfLinked(veterinarianCpf, tutorCpf);
//        return ResponseEntity.ok(animals);
//    }

    @GetMapping("/veterinarian/{veterinarianCpf}/tutor/{tutorCpf}/animals")
    public ResponseEntity<Set<AnimalResponseDTO>> getAnimalsForLinkedVet(
            @PathVariable String veterinarianCpf,
            @PathVariable String tutorCpf) {

        Set<AnimalResponseDTO> animals =
                linkService.getTutorAnimalsIfLinked(
                        veterinarianCpf,
                        tutorCpf
                );

        return ResponseEntity.ok(animals);
    }

    @GetMapping("/veterinarian/{veterinarianCpf}/tutors")
    public ResponseEntity<List<TutorResponseDTO>> getLinkedTutors(@PathVariable String veterinarianCpf) {
        List<TutorResponseDTO> tutors = linkService.getLinkedTutorsForVet(veterinarianCpf);
        return ResponseEntity.ok(tutors);
    }
}