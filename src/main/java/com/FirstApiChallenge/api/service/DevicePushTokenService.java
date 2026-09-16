package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.PushTokenRequestDTO;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.DevicePushToken;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.repository.DevicePushTokenRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DevicePushTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevicePushTokenService.class);

    private final DevicePushTokenRepository tokenRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarianRepository veterinarianRepository;

    public DevicePushTokenService(
            DevicePushTokenRepository tokenRepository,
            TutorRepository tutorRepository,
            VeterinarianRepository veterinarianRepository
    ) {
        this.tokenRepository = tokenRepository;
        this.tutorRepository = tutorRepository;
        this.veterinarianRepository = veterinarianRepository;
    }

    @Transactional
    public void registerTutorToken(String cpf, PushTokenRequestDTO request) {
        Tutor tutor = tutorRepository.findByCpf(cpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));
        String tokenValue = normalize(request.token());
        DevicePushToken token = tokenRepository.findByTokenForUpdate(tokenValue)
                .orElseGet(() -> new DevicePushToken(tokenValue, request.platform(), tutor));
        token.assignToTutor(tutor, request.platform());
        tokenRepository.save(token);
        LOGGER.info("Token push {} registrado para Tutor id={}", mask(tokenValue), tutor.getId());
    }

    @Transactional
    public void registerVeterinarianToken(String cpf, PushTokenRequestDTO request) {
        Veterinarian veterinarian = veterinarianRepository.findByCpf(cpf)
                .orElseThrow(() -> new CustomException("Veterinário não encontrado", HttpStatus.NOT_FOUND));
        String tokenValue = normalize(request.token());
        DevicePushToken token = tokenRepository.findByTokenForUpdate(tokenValue)
                .orElseGet(() -> new DevicePushToken(tokenValue, request.platform(), veterinarian));
        token.assignToVeterinarian(veterinarian, request.platform());
        tokenRepository.save(token);
        LOGGER.info("Token push {} registrado para Veterinário id={}", mask(tokenValue), veterinarian.getId());
    }

    @Transactional
    public void deactivateTutorToken(String cpf, String tokenValue) {
        Tutor tutor = tutorRepository.findByCpf(cpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));
        deactivateOwnedToken(normalize(tokenValue), tutor.getId(), null);
    }

    @Transactional
    public void deactivateVeterinarianToken(String cpf, String tokenValue) {
        Veterinarian veterinarian = veterinarianRepository.findByCpf(cpf)
                .orElseThrow(() -> new CustomException("Veterinário não encontrado", HttpStatus.NOT_FOUND));
        deactivateOwnedToken(normalize(tokenValue), null, veterinarian.getId());
    }

    private void deactivateOwnedToken(String tokenValue, Long tutorId, Long veterinarianId) {
        tokenRepository.findByTokenForUpdate(tokenValue)
                .filter(token -> belongsTo(token, tutorId, veterinarianId))
                .ifPresent(token -> {
                    token.deactivate();
                    tokenRepository.save(token);
                    LOGGER.info("Token push {} desativado", mask(tokenValue));
                });
    }

    private boolean belongsTo(DevicePushToken token, Long tutorId, Long veterinarianId) {
        return tutorId != null && token.getTutor() != null && tutorId.equals(token.getTutor().getId())
                || veterinarianId != null && token.getVeterinarian() != null
                && veterinarianId.equals(token.getVeterinarian().getId());
    }

    private String normalize(String token) {
        if (token == null || token.isBlank()) {
            throw new CustomException("Token push é obrigatório", HttpStatus.BAD_REQUEST);
        }
        return token.trim();
    }

    static String mask(String token) {
        if (token.length() <= 12) {
            return "***";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
