package com.FirstApiChallenge.api;

import com.FirstApiChallenge.api.dto.AppointmentRequestDTO;
import com.FirstApiChallenge.api.dto.MedicalRecordRequestDTO;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.AppointmentRepository;
import com.FirstApiChallenge.api.repository.MedicalRecordRepository;
import com.FirstApiChallenge.api.repository.NotificationRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import com.FirstApiChallenge.api.service.AppointmentService;
import com.FirstApiChallenge.api.service.MedicalRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class MedicalRecordServiceTests {

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private VeterinarianTutorLinkRepository linkRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Tutor tutor;
    private Veterinarian veterinarian;
    private Animal animal;
    private VeterinarianTutorLink link;

    @BeforeEach
    void setUp() {
        tutor = tutorRepository.save(createTutor("12345678901", "Tutor A"));
        veterinarian = veterinarianRepository.save(createVeterinarian("10987654321", "Vet B", "1234"));

        animal = new Animal();
        animal.setName("Luna");
        animal.setWeight(8.5f);
        animal.setHeight(0.4f);
        animal.setAge(3);
        animal.setRace("SRD");
        animal.setSpecies("Cachorro");
        animal.setHistory("");
        animal.setTutor(tutor);
        animal = animalRepository.save(animal);

        link = new VeterinarianTutorLink(veterinarian, tutor);
        link.setStatus(LinkStatus.ACCEPTED);
        link = linkRepository.save(link);
    }

    @Test
    void createsRecordCompletesAppointmentNotifiesTutorAndRejectsDuplicate() {
        Long appointmentId = createConfirmedAppointment();

        var response = medicalRecordService.create(
                appointmentId,
                veterinarian.getCpf(),
                request("Diagnóstico inicial")
        );

        assertNotNull(response.id());
        assertEquals(animal.getId(), response.animalId());
        assertEquals(
                AppointmentStatus.COMPLETED,
                appointmentRepository.findById(appointmentId).orElseThrow().getStatus()
        );
        assertTrue(medicalRecordRepository.existsByAppointmentId(appointmentId));
        assertFalse(notificationRepository.findByTutorCpfOrderByCreatedAtDesc(tutor.getCpf())
                .stream()
                .filter(notification -> notification.getType() == NotificationType.MEDICAL_RECORD_CREATED)
                .toList()
                .isEmpty());

        CustomException duplicate = assertThrows(CustomException.class, () -> medicalRecordService.create(
                appointmentId,
                veterinarian.getCpf(),
                request("Duplicado")
        ));
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatus());
    }

    @Test
    void rejectsPendingCancelledWrongVeterinarianAndInactiveLink() {
        var pending = appointmentService.create(
                tutor.getCpf(),
                new AppointmentRequestDTO(
                        animal.getId(),
                        veterinarian.getCpf(),
                        LocalDateTime.now().plusDays(1),
                        "Pendente"
                )
        );

        CustomException pendingError = assertThrows(CustomException.class, () -> medicalRecordService.create(
                pending.id(),
                veterinarian.getCpf(),
                request("Pendente")
        ));
        assertEquals(HttpStatus.BAD_REQUEST, pendingError.getStatus());

        appointmentService.cancelByVeterinarian(pending.id(), veterinarian.getCpf());
        CustomException cancelledError = assertThrows(CustomException.class, () -> medicalRecordService.create(
                pending.id(),
                veterinarian.getCpf(),
                request("Cancelada")
        ));
        assertEquals(HttpStatus.BAD_REQUEST, cancelledError.getStatus());

        Long confirmedId = createConfirmedAppointment();
        CustomException wrongVeterinarian = assertThrows(CustomException.class, () -> medicalRecordService.create(
                confirmedId,
                "00000000000",
                request("Outro veterinário")
        ));
        assertEquals(HttpStatus.FORBIDDEN, wrongVeterinarian.getStatus());

        link.setStatus(LinkStatus.REJECTED);
        linkRepository.save(link);
        CustomException inactiveLink = assertThrows(CustomException.class, () -> medicalRecordService.create(
                confirmedId,
                veterinarian.getCpf(),
                request("Sem vínculo")
        ));
        assertEquals(HttpStatus.FORBIDDEN, inactiveLink.getStatus());
    }

    @Test
    void authorizesHistoryAndAllowsOnlyResponsibleVeterinarianToEdit() {
        assertTrue(medicalRecordService.getAnimalHistoryForTutor(tutor.getCpf(), animal.getId()).isEmpty());
        assertTrue(medicalRecordService
                .getAnimalHistoryForVeterinarian(veterinarian.getCpf(), animal.getId())
                .isEmpty());

        Long appointmentId = createConfirmedAppointment();
        var created = medicalRecordService.create(
                appointmentId,
                veterinarian.getCpf(),
                request("Original")
        );

        assertEquals(1, medicalRecordService.getAnimalHistoryForTutor(tutor.getCpf(), animal.getId()).size());
        assertEquals(
                1,
                medicalRecordService
                        .getAnimalHistoryForVeterinarian(veterinarian.getCpf(), animal.getId())
                        .size()
        );

        Tutor otherTutor = tutorRepository.save(createTutor("22233344455", "Tutor C"));
        CustomException tutorDenied = assertThrows(
                CustomException.class,
                () -> medicalRecordService.getAnimalHistoryForTutor(otherTutor.getCpf(), animal.getId())
        );
        assertEquals(HttpStatus.FORBIDDEN, tutorDenied.getStatus());

        Veterinarian otherVeterinarian = veterinarianRepository.save(
                createVeterinarian("33344455566", "Vet D", "5678")
        );
        CustomException veterinarianDenied = assertThrows(
                CustomException.class,
                () -> medicalRecordService.getAnimalHistoryForVeterinarian(
                        otherVeterinarian.getCpf(),
                        animal.getId()
                )
        );
        assertEquals(HttpStatus.FORBIDDEN, veterinarianDenied.getStatus());

        CustomException editDenied = assertThrows(CustomException.class, () -> medicalRecordService.update(
                created.id(),
                otherVeterinarian.getCpf(),
                request("Alteração indevida")
        ));
        assertEquals(HttpStatus.FORBIDDEN, editDenied.getStatus());

        var updated = medicalRecordService.update(
                created.id(),
                veterinarian.getCpf(),
                request("Diagnóstico atualizado")
        );
        assertEquals("Diagnóstico atualizado", updated.diagnosis());
        assertEquals(appointmentId, updated.appointmentId());
        assertEquals(animal.getId(), updated.animalId());
        assertNotNull(updated.updatedAt());

        link.setStatus(LinkStatus.REJECTED);
        linkRepository.save(link);
        CustomException inactiveLinkEdit = assertThrows(CustomException.class, () -> medicalRecordService.update(
                created.id(),
                veterinarian.getCpf(),
                request("Sem vínculo")
        ));
        assertEquals(HttpStatus.FORBIDDEN, inactiveLinkEdit.getStatus());
    }

    private Long createConfirmedAppointment() {
        var appointment = appointmentService.create(
                tutor.getCpf(),
                new AppointmentRequestDTO(
                        animal.getId(),
                        veterinarian.getCpf(),
                        LocalDateTime.now().plusDays(2),
                        "Consulta"
                )
        );
        return appointmentService.confirm(appointment.id(), veterinarian.getCpf()).id();
    }

    private MedicalRecordRequestDTO request(String diagnosis) {
        return new MedicalRecordRequestDTO(
                diagnosis,
                "Descrição clínica",
                8.7f,
                38.5f,
                "Observações"
        );
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
