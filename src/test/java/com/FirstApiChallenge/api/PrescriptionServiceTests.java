package com.FirstApiChallenge.api;

import com.FirstApiChallenge.api.dto.AppointmentRequestDTO;
import com.FirstApiChallenge.api.dto.MedicalRecordRequestDTO;
import com.FirstApiChallenge.api.dto.PrescriptionItemRequestDTO;
import com.FirstApiChallenge.api.dto.PrescriptionRequestDTO;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.NotificationRepository;
import com.FirstApiChallenge.api.repository.PrescriptionRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import com.FirstApiChallenge.api.service.AppointmentService;
import com.FirstApiChallenge.api.service.MedicalRecordService;
import com.FirstApiChallenge.api.service.PrescriptionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PrescriptionServiceTests {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

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

    @Autowired
    private EntityManager entityManager;

    private Tutor tutor;
    private Veterinarian veterinarian;
    private Animal animal;
    private VeterinarianTutorLink link;
    private Long medicalRecordId;

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

        var appointment = appointmentService.create(
                tutor.getCpf(),
                new AppointmentRequestDTO(
                        animal.getId(),
                        veterinarian.getCpf(),
                        LocalDateTime.now().plusDays(1),
                        "Consulta"
                )
        );
        appointmentService.confirm(appointment.id(), veterinarian.getCpf());

        medicalRecordId = medicalRecordService.create(
                appointment.id(),
                veterinarian.getCpf(),
                new MedicalRecordRequestDTO(
                        "Diagnóstico",
                        "Descrição",
                        8.5f,
                        38.5f,
                        null
                )
        ).id();
    }

    @Test
    void createsPrescriptionWithMultipleItemsNotifiesTutorAndRejectsDuplicate() {
        var response = prescriptionService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                request("Amoxicilina", "Dipirona")
        );

        assertEquals(medicalRecordId, response.medicalRecordId());
        assertEquals(2, response.items().size());
        assertTrue(prescriptionRepository.existsByMedicalRecordId(medicalRecordId));
        assertFalse(notificationRepository.findByTutorCpfOrderByCreatedAtDesc(tutor.getCpf())
                .stream()
                .filter(notification -> notification.getType() == NotificationType.PRESCRIPTION_CREATED)
                .toList()
                .isEmpty());

        CustomException duplicate = assertThrows(CustomException.class, () -> prescriptionService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                request("Prednisona")
        ));
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatus());
    }

    @Test
    void rejectsEmptyItemsWrongVeterinarianAndInactiveLink() {
        CustomException emptyItems = assertThrows(CustomException.class, () -> prescriptionService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                new PrescriptionRequestDTO(null, List.of())
        ));
        assertEquals(HttpStatus.BAD_REQUEST, emptyItems.getStatus());

        CustomException wrongVeterinarian = assertThrows(CustomException.class, () -> prescriptionService.create(
                medicalRecordId,
                "00000000000",
                request("Amoxicilina")
        ));
        assertEquals(HttpStatus.FORBIDDEN, wrongVeterinarian.getStatus());

        link.setStatus(LinkStatus.REJECTED);
        linkRepository.save(link);
        CustomException inactiveLink = assertThrows(CustomException.class, () -> prescriptionService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                request("Amoxicilina")
        ));
        assertEquals(HttpStatus.FORBIDDEN, inactiveLink.getStatus());
    }

    @Test
    void authorizesReadingAndReplacesItemsOnUpdate() {
        assertTrue(prescriptionService.getForTutor(tutor.getCpf(), medicalRecordId).isEmpty());
        assertTrue(prescriptionService
                .getForVeterinarian(veterinarian.getCpf(), medicalRecordId)
                .isEmpty());

        var created = prescriptionService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                request("Amoxicilina", "Dipirona")
        );

        assertTrue(prescriptionService.getForTutor(tutor.getCpf(), medicalRecordId).isPresent());
        assertTrue(prescriptionService
                .getForVeterinarian(veterinarian.getCpf(), medicalRecordId)
                .isPresent());

        Tutor otherTutor = tutorRepository.save(createTutor("22233344455", "Tutor C"));
        CustomException tutorDenied = assertThrows(
                CustomException.class,
                () -> prescriptionService.getForTutor(otherTutor.getCpf(), medicalRecordId)
        );
        assertEquals(HttpStatus.FORBIDDEN, tutorDenied.getStatus());

        Veterinarian otherVeterinarian = veterinarianRepository.save(
                createVeterinarian("33344455566", "Vet D", "5678")
        );
        CustomException veterinarianDenied = assertThrows(
                CustomException.class,
                () -> prescriptionService.getForVeterinarian(
                        otherVeterinarian.getCpf(),
                        medicalRecordId
                )
        );
        assertEquals(HttpStatus.FORBIDDEN, veterinarianDenied.getStatus());

        var updated = prescriptionService.update(
                created.id(),
                veterinarian.getCpf(),
                request("Amoxicilina", "Prednisona")
        );
        assertEquals(2, updated.items().size());
        assertTrue(updated.items().stream().anyMatch(item -> item.medicationName().equals("Prednisona")));
        assertFalse(updated.items().stream().anyMatch(item -> item.medicationName().equals("Dipirona")));

        entityManager.clear();
        var persisted = prescriptionRepository.findById(created.id()).orElseThrow();
        assertEquals(2, persisted.getItems().size());
        assertEquals(
                List.of("Amoxicilina", "Prednisona"),
                persisted.getItems().stream().map(item -> item.getMedicationName()).toList()
        );
    }

    private PrescriptionRequestDTO request(String... medications) {
        List<PrescriptionItemRequestDTO> items = List.of(medications).stream()
                .map(name -> new PrescriptionItemRequestDTO(
                        name,
                        "250 mg",
                        "A cada 8 horas",
                        "7 dias",
                        "Oral",
                        "Após alimentação"
                ))
                .toList();

        return new PrescriptionRequestDTO("Manter hidratado", items);
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
