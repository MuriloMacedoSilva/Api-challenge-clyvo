package com.FirstApiChallenge.api;

import com.FirstApiChallenge.api.dto.PushTokenRequestDTO;
import com.FirstApiChallenge.api.enums.DevicePlatform;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.repository.DevicePushTokenRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.service.DevicePushTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DevicePushTokenServiceTests {

    @Autowired
    private DevicePushTokenService tokenService;

    @Autowired
    private DevicePushTokenRepository tokenRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private MockMvc mockMvc;

    private Tutor tutor;
    private Veterinarian veterinarian;

    @BeforeEach
    void setUp() {
        tutor = tutorRepository.save(createTutor("12345678901"));
        veterinarian = veterinarianRepository.save(createVeterinarian("98765432100"));
    }

    @Test
    void registersTokenForTutor() {
        tokenService.registerTutorToken(tutor.getCpf(), request("ExponentPushToken[tutor]"));

        var token = tokenRepository.findByToken("ExponentPushToken[tutor]").orElseThrow();
        assertEquals(tutor.getId(), token.getTutor().getId());
        assertTrue(token.isActive());
    }

    @Test
    void registersTokenForVeterinarian() {
        tokenService.registerVeterinarianToken(veterinarian.getCpf(), request("ExponentPushToken[vet]"));

        var token = tokenRepository.findByToken("ExponentPushToken[vet]").orElseThrow();
        assertEquals(veterinarian.getId(), token.getVeterinarian().getId());
        assertTrue(token.isActive());
    }

    @Test
    void rejectsUnknownOwnerAndEmptyToken() {
        CustomException missing = assertThrows(
                CustomException.class,
                () -> tokenService.registerTutorToken("00000000000", request("ExponentPushToken[x]"))
        );
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatus());

        CustomException empty = assertThrows(
                CustomException.class,
                () -> tokenService.registerTutorToken(
                        tutor.getCpf(),
                        new PushTokenRequestDTO("   ", DevicePlatform.ANDROID)
                )
        );
        assertEquals(HttpStatus.BAD_REQUEST, empty.getStatus());
    }

    @Test
    void endpointReturnsNotFoundForUnknownUserAndBadRequestForEmptyToken() throws Exception {
        mockMvc.perform(post("/v1/push/tutor/00000000000/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"ExponentPushToken[x]\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/v1/push/tutor/{cpf}/tokens", tutor.getCpf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateRegistrationIsIdempotent() {
        tokenService.registerTutorToken(tutor.getCpf(), request("ExponentPushToken[same]"));
        tokenService.registerTutorToken(tutor.getCpf(), request("ExponentPushToken[same]"));

        assertEquals(1, tokenRepository.count());
    }

    @Test
    void supportsMultipleTokensAndDeactivatesOnlyCurrentDevice() {
        tokenService.registerTutorToken(tutor.getCpf(), request("ExponentPushToken[one]"));
        tokenService.registerTutorToken(tutor.getCpf(), request("ExponentPushToken[two]"));

        tokenService.deactivateTutorToken(tutor.getCpf(), "ExponentPushToken[one]");

        assertFalse(tokenRepository.findByToken("ExponentPushToken[one]").orElseThrow().isActive());
        assertTrue(tokenRepository.findByToken("ExponentPushToken[two]").orElseThrow().isActive());
        assertEquals(1, tokenRepository.findByTutorIdAndActiveTrue(tutor.getId()).size());
    }

    @Test
    void reassignsSameDeviceTokenToNewAccount() {
        tokenService.registerTutorToken(tutor.getCpf(), request("ExponentPushToken[shared]"));
        tokenService.registerVeterinarianToken(veterinarian.getCpf(), request("ExponentPushToken[shared]"));

        var token = tokenRepository.findByToken("ExponentPushToken[shared]").orElseThrow();
        assertEquals(veterinarian.getId(), token.getVeterinarian().getId());
        assertEquals(null, token.getTutor());
        assertTrue(tokenRepository.findByTutorIdAndActiveTrue(tutor.getId()).isEmpty());
        assertEquals(1, tokenRepository.findByVeterinarianIdAndActiveTrue(veterinarian.getId()).size());
    }

    @Test
    void invalidResultForPreviousOwnerDoesNotDeactivateReassignedToken() {
        String tokenValue = "ExponentPushToken[reassigned]";
        tokenService.registerTutorToken(tutor.getCpf(), request(tokenValue));
        tokenService.registerVeterinarianToken(veterinarian.getCpf(), request(tokenValue));

        int updated = tokenRepository.deactivateActiveTutorToken(tokenValue, tutor.getId());

        assertEquals(0, updated);
        var token = tokenRepository.findByToken(tokenValue).orElseThrow();
        assertTrue(token.isActive());
        assertEquals(veterinarian.getId(), token.getVeterinarian().getId());
    }

    private PushTokenRequestDTO request(String token) {
        return new PushTokenRequestDTO(token, DevicePlatform.ANDROID);
    }

    private Tutor createTutor(String cpf) {
        Tutor value = new Tutor();
        value.setName("Tutor Push");
        value.setEmail("tutor@push.test");
        value.setCpf(cpf);
        value.setPhoneNumber("11999999999");
        value.setPassword("12345678");
        value.setRole("tutor");
        return value;
    }

    private Veterinarian createVeterinarian(String cpf) {
        Veterinarian value = new Veterinarian();
        value.setName("Veterinário Push");
        value.setEmail("vet@push.test");
        value.setCpf(cpf);
        value.setPhoneNumber("11999999999");
        value.setPassword("12345678");
        value.setRole("veterinarian");
        value.setCrmvNumber("12345");
        value.setCrmvState("SP");
        value.setCnpj("12345678000199");
        return value;
    }
}
