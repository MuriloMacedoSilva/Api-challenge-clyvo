package com.FirstApiChallenge.api;

import com.FirstApiChallenge.api.dto.AppointmentRequestDTO;
import com.FirstApiChallenge.api.dto.ExamRequestDTO;
import com.FirstApiChallenge.api.dto.ExamResultRequestDTO;
import com.FirstApiChallenge.api.dto.MedicalRecordRequestDTO;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.AppointmentRepository;
import com.FirstApiChallenge.api.repository.ExamRepository;
import com.FirstApiChallenge.api.repository.MedicalRecordRepository;
import com.FirstApiChallenge.api.repository.NotificationRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import com.FirstApiChallenge.api.service.AppointmentService;
import com.FirstApiChallenge.api.service.ExamService;
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
class ExamServiceTests {

    @Autowired
    private ExamService examService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

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
    void createsMultipleRequestedExamsAndNotifiesTutor() {
        assertTrue(examService.getByMedicalRecordForTutor(tutor.getCpf(), medicalRecordId).isEmpty());
        assertTrue(examService.getByAnimalForTutor(tutor.getCpf(), animal.getId()).isEmpty());

        var bloodTest = examService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                request("Hemograma completo")
        );
        var ultrasound = examService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                request("Ultrassonografia abdominal")
        );
        var xray = examService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                request("Raio-X de tórax")
        );

        assertEquals(ExamStatus.REQUESTED, bloodTest.status());
        assertNotNull(bloodTest.requestDate());
        assertEquals(3, examService.getByMedicalRecordForTutor(tutor.getCpf(), medicalRecordId).size());
        assertEquals(3, examService.getByAnimalForTutor(tutor.getCpf(), animal.getId()).size());
        assertEquals(medicalRecordId, ultrasound.medicalRecordId());
        assertEquals(animal.getId(), xray.animalId());
        assertFalse(notificationRepository.findByTutorCpfOrderByCreatedAtDesc(tutor.getCpf())
                .stream()
                .filter(notification -> notification.getType() == NotificationType.EXAM_REQUESTED)
                .toList()
                .isEmpty());
    }

    @Test
    void registersResultAndRejectsInvalidTransitions() {
        var exam = examService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                request("Hemograma completo")
        );

        CustomException emptyResult = assertThrows(CustomException.class, () -> examService.registerResult(
                exam.id(),
                veterinarian.getCpf(),
                new ExamResultRequestDTO(" ", null)
        ));
        assertEquals(HttpStatus.BAD_REQUEST, emptyResult.getStatus());

        var completed = examService.registerResult(
                exam.id(),
                veterinarian.getCpf(),
                new ExamResultRequestDTO("Resultado dentro dos parâmetros", "Sem alterações")
        );
        assertEquals(ExamStatus.COMPLETED, completed.status());
        assertNotNull(completed.resultDate());

        CustomException repeatedResult = assertThrows(CustomException.class, () -> examService.registerResult(
                exam.id(),
                veterinarian.getCpf(),
                new ExamResultRequestDTO("Outro resultado", null)
        ));
        assertEquals(HttpStatus.BAD_REQUEST, repeatedResult.getStatus());

        CustomException completedEdit = assertThrows(CustomException.class, () -> examService.update(
                exam.id(),
                veterinarian.getCpf(),
                request("Alteração após resultado")
        ));
        assertEquals(HttpStatus.BAD_REQUEST, completedEdit.getStatus());

        CustomException completedCancellation = assertThrows(
                CustomException.class,
                () -> examService.cancel(exam.id(), veterinarian.getCpf())
        );
        assertEquals(HttpStatus.BAD_REQUEST, completedCancellation.getStatus());
        assertFalse(notificationRepository.findByTutorCpfOrderByCreatedAtDesc(tutor.getCpf())
                .stream()
                .filter(notification -> notification.getType() == NotificationType.EXAM_RESULT_AVAILABLE)
                .toList()
                .isEmpty());

        var cancellable = examService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                request("Exame de urina")
        );
        assertEquals(
                ExamStatus.CANCELLED,
                examService.cancel(cancellable.id(), veterinarian.getCpf()).status()
        );
        assertThrows(
                CustomException.class,
                () -> examService.update(cancellable.id(), veterinarian.getCpf(), request("Cancelado"))
        );
        assertThrows(
                CustomException.class,
                () -> examService.cancel(cancellable.id(), veterinarian.getCpf())
        );
    }

    @Test
    void controlsMutationAndHistoryAccess() {
        var exam = examService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                request("Hemograma")
        );

        var updated = examService.update(
                exam.id(),
                veterinarian.getCpf(),
                new ExamRequestDTO("Hemograma completo", "Laboratorial", "Jejum")
        );
        assertEquals("Hemograma completo", updated.examName());

        CustomException wrongVeterinarian = assertThrows(CustomException.class, () -> examService.update(
                exam.id(),
                "00000000000",
                request("Alteração indevida")
        ));
        assertEquals(HttpStatus.FORBIDDEN, wrongVeterinarian.getStatus());

        Tutor otherTutor = tutorRepository.save(createTutor("22233344455", "Tutor C"));
        CustomException tutorDenied = assertThrows(
                CustomException.class,
                () -> examService.getByMedicalRecordForTutor(otherTutor.getCpf(), medicalRecordId)
        );
        assertEquals(HttpStatus.FORBIDDEN, tutorDenied.getStatus());

        Veterinarian otherVeterinarian = veterinarianRepository.save(
                createVeterinarian("33344455566", "Vet D", "5678")
        );
        CustomException veterinarianDenied = assertThrows(
                CustomException.class,
                () -> examService.getByAnimalForVeterinarian(
                        otherVeterinarian.getCpf(),
                        animal.getId()
                )
        );
        assertEquals(HttpStatus.FORBIDDEN, veterinarianDenied.getStatus());

        assertEquals(
                1,
                examService.getByMedicalRecordForVeterinarian(
                        veterinarian.getCpf(),
                        medicalRecordId
                ).size()
        );
        assertEquals(
                1,
                examService.getByAnimalForVeterinarian(veterinarian.getCpf(), animal.getId()).size()
        );

        var record = medicalRecordRepository.findById(medicalRecordId).orElseThrow();
        record.getAppointment().setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.saveAndFlush(record.getAppointment());
        CustomException incompleteAppointment = assertThrows(CustomException.class, () -> examService.create(
                medicalRecordId,
                veterinarian.getCpf(),
                request("Consulta não concluída")
        ));
        assertEquals(HttpStatus.BAD_REQUEST, incompleteAppointment.getStatus());

        link.setStatus(LinkStatus.REJECTED);
        linkRepository.save(link);
        CustomException inactiveLink = assertThrows(
                CustomException.class,
                () -> examService.registerResult(
                        exam.id(),
                        veterinarian.getCpf(),
                        new ExamResultRequestDTO("Resultado", null)
                )
        );
        assertEquals(HttpStatus.FORBIDDEN, inactiveLink.getStatus());
        assertEquals(ExamStatus.REQUESTED, examRepository.findById(exam.id()).orElseThrow().getStatus());
    }

    private ExamRequestDTO request(String examName) {
        return new ExamRequestDTO(examName, "Laboratorial", "Jejum de 8 horas");
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
