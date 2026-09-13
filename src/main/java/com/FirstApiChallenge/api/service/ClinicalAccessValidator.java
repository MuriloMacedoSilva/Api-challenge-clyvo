package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ClinicalAccessValidator {

    private final VeterinarianTutorLinkRepository linkRepository;

    public ClinicalAccessValidator(VeterinarianTutorLinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    public void requireAcceptedLink(Veterinarian veterinarian, Tutor tutor) {
        boolean hasAcceptedLink = linkRepository.existsByVeterinarianCpfAndTutorCpfAndStatus(
                veterinarian.getCpf(),
                tutor.getCpf(),
                LinkStatus.ACCEPTED
        );

        if (!hasAcceptedLink) {
            throw new CustomException(
                    "Veterinário não possui vínculo aceito com o tutor deste animal",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    public void requireTutorOwnership(Tutor tutor, Animal animal) {
        if (!animal.getTutor().getId().equals(tutor.getId())) {
            throw new CustomException(
                    "Este animal não pertence ao tutor informado",
                    HttpStatus.FORBIDDEN
            );
        }
    }
}
