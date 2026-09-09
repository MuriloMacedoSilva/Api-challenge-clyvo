package com.FirstApiChallenge.api;

import com.FirstApiChallenge.api.dto.AppointmentRequestDTO;
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
import com.FirstApiChallenge.api.repository.NotificationRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import com.FirstApiChallenge.api.service.AppointmentService;
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
class AppointmentServiceTests {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private VeterinarianTutorLinkRepository linkRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Tutor tutor;
    private Veterinarian veterinarian;
    private Animal animal;

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

        VeterinarianTutorLink link = new VeterinarianTutorLink(veterinarian, tutor);
        link.setStatus(LinkStatus.ACCEPTED);
        linkRepository.save(link);
    }

    @Test
    void createsPendingAppointmentAndNotifiesVeterinarian() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);

        assertTrue(appointmentService.getTutorAppointments(tutor.getCpf()).isEmpty());
        assertTrue(appointmentService.getVeterinarianAppointments(veterinarian.getCpf()).isEmpty());
        assertEquals(1, appointmentService.getLinkedVeterinarians(tutor.getCpf()).size());

        var response = appointmentService.create(
                tutor.getCpf(),
                new AppointmentRequestDTO(animal.getId(), veterinarian.getCpf(), scheduledAt, "Rotina")
        );

        assertNotNull(response.id());
        assertEquals(AppointmentStatus.PENDING, response.status());
        assertEquals(animal.getId(), response.animalId());
        assertFalse(notificationRepository.findByVeterinarianCpfOrderByCreatedAtDesc(veterinarian.getCpf())
                .stream()
                .filter(notification -> notification.getType() == NotificationType.APPOINTMENT_REQUESTED)
                .toList()
                .isEmpty());
    }

    @Test
    void rejectsInactiveLinkOtherTutorAnimalPastDateAndScheduleConflict() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        appointmentService.create(
                tutor.getCpf(),
                new AppointmentRequestDTO(animal.getId(), veterinarian.getCpf(), scheduledAt, "Primeira")
        );

        CustomException conflict = assertThrows(CustomException.class, () -> appointmentService.create(
                tutor.getCpf(),
                new AppointmentRequestDTO(animal.getId(), veterinarian.getCpf(), scheduledAt, "Conflito")
        ));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatus());

        CustomException pastDate = assertThrows(CustomException.class, () -> appointmentService.create(
                tutor.getCpf(),
                new AppointmentRequestDTO(
                        animal.getId(),
                        veterinarian.getCpf(),
                        LocalDateTime.now().minusHours(1),
                        "Passado"
                )
        ));
        assertEquals(HttpStatus.BAD_REQUEST, pastDate.getStatus());

        Tutor otherTutor = tutorRepository.save(createTutor("22233344455", "Tutor C"));
        CustomException ownership = assertThrows(CustomException.class, () -> appointmentService.create(
                otherTutor.getCpf(),
                new AppointmentRequestDTO(
                        animal.getId(),
                        veterinarian.getCpf(),
                        scheduledAt.plusHours(1),
                        "Animal alheio"
                )
        ));
        assertEquals(HttpStatus.FORBIDDEN, ownership.getStatus());

        Veterinarian otherVeterinarian = veterinarianRepository.save(
                createVeterinarian("33344455566", "Vet D", "5678")
        );
        CustomException linkDenied = assertThrows(CustomException.class, () -> appointmentService.create(
                tutor.getCpf(),
                new AppointmentRequestDTO(
                        animal.getId(),
                        otherVeterinarian.getCpf(),
                        scheduledAt.plusHours(2),
                        "Sem vínculo"
                )
        ));
        assertEquals(HttpStatus.FORBIDDEN, linkDenied.getStatus());
    }

    @Test
    void confirmsAndCancelsOnlyWithRelatedUsers() {
        var pending = appointmentService.create(
                tutor.getCpf(),
                new AppointmentRequestDTO(
                        animal.getId(),
                        veterinarian.getCpf(),
                        LocalDateTime.now().plusDays(1),
                        "Retorno"
                )
        );

        CustomException wrongVeterinarian = assertThrows(
                CustomException.class,
                () -> appointmentService.confirm(pending.id(), "00000000000")
        );
        assertEquals(HttpStatus.FORBIDDEN, wrongVeterinarian.getStatus());

        assertEquals(
                AppointmentStatus.CONFIRMED,
                appointmentService.confirm(pending.id(), veterinarian.getCpf()).status()
        );
        assertFalse(notificationRepository.findByTutorCpfOrderByCreatedAtDesc(tutor.getCpf())
                .stream()
                .filter(notification -> notification.getType() == NotificationType.APPOINTMENT_CONFIRMED)
                .toList()
                .isEmpty());
        assertEquals(
                AppointmentStatus.CANCELLED,
                appointmentService.cancelByVeterinarian(pending.id(), veterinarian.getCpf()).status()
        );

        CustomException repeatedCancellation = assertThrows(
                CustomException.class,
                () -> appointmentService.cancelByTutor(pending.id(), tutor.getCpf())
        );
        assertEquals(HttpStatus.BAD_REQUEST, repeatedCancellation.getStatus());

        var cancellable = appointmentService.create(
                tutor.getCpf(),
                new AppointmentRequestDTO(
                        animal.getId(),
                        veterinarian.getCpf(),
                        LocalDateTime.now().plusDays(2),
                        "Vacina"
                )
        );
        assertEquals(
                AppointmentStatus.CANCELLED,
                appointmentService.cancelByTutor(cancellable.id(), tutor.getCpf()).status()
        );

        var veterinarianCancellable = appointmentService.create(
                tutor.getCpf(),
                new AppointmentRequestDTO(
                        animal.getId(),
                        veterinarian.getCpf(),
                        LocalDateTime.now().plusDays(3),
                        "Avaliação"
                )
        );
        assertEquals(
                AppointmentStatus.CANCELLED,
                appointmentService.cancelByVeterinarian(
                        veterinarianCancellable.id(),
                        veterinarian.getCpf()
                ).status()
        );
        assertEquals(3, appointmentRepository.findByTutorCpfOrderByScheduledAtAsc(tutor.getCpf()).size());
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
