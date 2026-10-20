package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.client.ClyvoAiClient;
import com.FirstApiChallenge.api.dto.ClyvoAiRequestDTO;
import com.FirstApiChallenge.api.dto.ClyvoAiResponseDTO;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.*;
import com.FirstApiChallenge.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiOrchestrationServiceTests {

    @Mock private TutorRepository tutorRepository;
    @Mock private AnimalRepository animalRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private ExamRepository examRepository;
    @Mock private VaccinationRepository vaccinationRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ClyvoAiClient clyvoAiClient;

    private AiOrchestrationService service;
    private Tutor tutor;
    private Animal animal;
    private Veterinarian veterinarian;

    @BeforeEach
    void setUp() {
        service = new AiOrchestrationService(
                tutorRepository,
                animalRepository,
                medicalRecordRepository,
                prescriptionRepository,
                examRepository,
                vaccinationRepository,
                appointmentRepository,
                clyvoAiClient
        );

        tutor = new Tutor();
        tutor.setId(1L);
        tutor.setCpf("12345678901");
        tutor.setName("Tutor confidencial");
        tutor.setPassword("nao-enviar");

        animal = new Animal();
        animal.setId(10L);
        animal.setName("Luna");
        animal.setSpecies("Cachorro");
        animal.setRace("SRD");
        animal.setAge(4);
        animal.setWeight(9.5f);
        animal.setTutor(tutor);

        veterinarian = new Veterinarian();
        veterinarian.setId(20L);
        veterinarian.setName("Dra. Ana");
        veterinarian.setCpf("10987654321");
    }

    @Test
    void returnsNotFoundWhenTutorDoesNotExist() {
        when(tutorRepository.findByCpf("missing")).thenReturn(Optional.empty());

        CustomException error = assertThrows(CustomException.class,
                () -> service.askTutor("missing", 10L, "Pergunta"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verifyNoInteractions(animalRepository, clyvoAiClient);
    }

    @Test
    void returnsNotFoundWhenAnimalDoesNotExist() {
        when(tutorRepository.findByCpf(tutor.getCpf())).thenReturn(Optional.of(tutor));
        when(animalRepository.findById(99L)).thenReturn(Optional.empty());

        CustomException error = assertThrows(CustomException.class,
                () -> service.askTutor(tutor.getCpf(), 99L, "Pergunta"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verifyNoInteractions(clyvoAiClient);
    }

    @Test
    void returnsForbiddenWhenAnimalBelongsToAnotherTutor() {
        Tutor owner = new Tutor();
        owner.setId(2L);
        animal.setTutor(owner);
        when(tutorRepository.findByCpf(tutor.getCpf())).thenReturn(Optional.of(tutor));
        when(animalRepository.findById(animal.getId())).thenReturn(Optional.of(animal));

        CustomException error = assertThrows(CustomException.class,
                () -> service.askTutor(tutor.getCpf(), animal.getId(), "Pergunta"));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verifyNoInteractions(clyvoAiClient);
    }

    @Test
    void rejectsBlankQuestion() {
        CustomException error = assertThrows(CustomException.class,
                () -> service.askTutor(tutor.getCpf(), animal.getId(), "  "));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(tutorRepository, clyvoAiClient);
    }

    @Test
    void rejectsQuestionLongerThanTwoThousandCharacters() {
        CustomException error = assertThrows(CustomException.class,
                () -> service.askTutor(tutor.getCpf(), animal.getId(), "a".repeat(2001)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(tutorRepository, clyvoAiClient);
    }

    @Test
    void mapsOnlyRequiredAnimalContextAndDerivesTutorActor() {
        stubAuthorizedEmptyContext();

        service.askTutor(tutor.getCpf(), animal.getId(), "  Resuma a Luna  ");

        ClyvoAiRequestDTO request = capturedRequest();
        assertEquals("Resuma a Luna", request.question());
        assertEquals("TUTOR", request.actorType());
        assertEquals(animal.getId(), request.animal().id());
        assertEquals("Luna", request.animal().name());
        assertEquals("Cachorro", request.animal().species());
        assertEquals("SRD", request.animal().breed());
        assertEquals(4, request.animal().age());
        assertEquals(9.5f, request.animal().weight());
    }

    @Test
    void requestsAtMostFiveMedicalRecordsAndMapsThemNewestFirst() {
        stubAuthorizedEmptyContext();
        List<MedicalRecord> records = List.of(record(5L), record(4L), record(3L), record(2L), record(1L));
        when(medicalRecordRepository.findByAnimalIdOrderByCreatedAtDesc(eq(animal.getId()), any(Pageable.class)))
                .thenReturn(records);

        service.askTutor(tutor.getCpf(), animal.getId(), "Pergunta");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(medicalRecordRepository).findByAnimalIdOrderByCreatedAtDesc(eq(animal.getId()), pageable.capture());
        assertEquals(5, pageable.getValue().getPageSize());
        assertEquals(List.of(5L, 4L, 3L, 2L, 1L), capturedRequest().medicalRecords().stream().map(r -> r.id()).toList());
    }

    @Test
    void requestsAtMostFiveExams() {
        stubAuthorizedEmptyContext();
        when(examRepository.findByMedicalRecordAnimalIdOrderByRequestDateDesc(eq(animal.getId()), any(Pageable.class)))
                .thenReturn(List.of(exam(5L), exam(4L)));

        service.askTutor(tutor.getCpf(), animal.getId(), "Pergunta");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(examRepository).findByMedicalRecordAnimalIdOrderByRequestDateDesc(eq(animal.getId()), pageable.capture());
        assertEquals(5, pageable.getValue().getPageSize());
        assertEquals(List.of(5L, 4L), capturedRequest().exams().stream().map(e -> e.id()).toList());
    }

    @Test
    void requestsAtMostTenVaccinations() {
        stubAuthorizedEmptyContext();
        when(vaccinationRepository.findByAnimalIdOrderByApplicationDateDesc(eq(animal.getId()), any(Pageable.class)))
                .thenReturn(List.of(vaccination(2L), vaccination(1L)));

        service.askTutor(tutor.getCpf(), animal.getId(), "Pergunta");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(vaccinationRepository).findByAnimalIdOrderByApplicationDateDesc(eq(animal.getId()), pageable.capture());
        assertEquals(10, pageable.getValue().getPageSize());
        assertEquals(2, capturedRequest().vaccinations().size());
    }

    @Test
    void limitsCombinedAppointmentsToFiveAndExcludesCancelledQuery() {
        stubAuthorizedEmptyContext();
        when(appointmentRepository.findTop3ByAnimalIdAndScheduledAtGreaterThanEqualAndStatusInOrderByScheduledAtAsc(
                eq(animal.getId()), any(LocalDateTime.class), anyCollection()))
                .thenReturn(List.of(appointment(1L, AppointmentStatus.PENDING), appointment(2L, AppointmentStatus.CONFIRMED), appointment(3L, AppointmentStatus.CONFIRMED)));
        when(appointmentRepository.findTop5ByAnimalIdAndScheduledAtLessThanAndStatusOrderByScheduledAtDesc(
                eq(animal.getId()), any(LocalDateTime.class), eq(AppointmentStatus.COMPLETED)))
                .thenReturn(List.of(appointment(4L, AppointmentStatus.COMPLETED), appointment(5L, AppointmentStatus.COMPLETED), appointment(6L, AppointmentStatus.COMPLETED)));

        service.askTutor(tutor.getCpf(), animal.getId(), "Pergunta");

        assertEquals(5, capturedRequest().appointments().size());
        assertTrue(capturedRequest().appointments().stream().noneMatch(a -> "CANCELLED".equals(a.status())));
    }

    @Test
    void mapsPrescriptionAndItemsFromSelectedMedicalRecords() {
        stubAuthorizedEmptyContext();
        MedicalRecord record = record(30L);
        when(medicalRecordRepository.findByAnimalIdOrderByCreatedAtDesc(eq(animal.getId()), any(Pageable.class)))
                .thenReturn(List.of(record));
        Prescription prescription = prescription(40L, record);
        when(prescriptionRepository.findByMedicalRecordIdIn(List.of(30L))).thenReturn(List.of(prescription));

        service.askTutor(tutor.getCpf(), animal.getId(), "Pergunta");

        var mapped = capturedRequest().prescriptions().getFirst();
        assertEquals(40L, mapped.id());
        assertEquals("Dra. Ana", mapped.veterinarianName());
        assertEquals("Amoxicilina", mapped.items().getFirst().medication());
        assertEquals("oral", mapped.items().getFirst().route());
        assertEquals("Após alimentação", mapped.items().getFirst().instructions());
    }

    @Test
    void allClinicalQueriesAreRestrictedToRequestedAnimal() {
        stubAuthorizedEmptyContext();

        service.askTutor(tutor.getCpf(), animal.getId(), "Pergunta");

        verify(medicalRecordRepository).findByAnimalIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class));
        verify(examRepository).findByMedicalRecordAnimalIdOrderByRequestDateDesc(eq(10L), any(Pageable.class));
        verify(vaccinationRepository).findByAnimalIdOrderByApplicationDateDesc(eq(10L), any(Pageable.class));
        verify(appointmentRepository).findTop3ByAnimalIdAndScheduledAtGreaterThanEqualAndStatusInOrderByScheduledAtAsc(eq(10L), any(), anyCollection());
        verify(appointmentRepository).findTop5ByAnimalIdAndScheduledAtLessThanAndStatusOrderByScheduledAtDesc(eq(10L), any(), any());
        verifyNoMoreInteractions(animalRepository);
    }

    @Test
    void callsClientAndReturnsApplicationResponse() {
        stubAuthorizedEmptyContext();
        when(clyvoAiClient.chat(any())).thenReturn(new ClyvoAiResponseDTO("Resposta", "Aviso", "gemma3:4b"));

        var response = service.askTutor(tutor.getCpf(), animal.getId(), "Pergunta");

        assertEquals("Resposta", response.answer());
        assertEquals("Aviso", response.disclaimer());
        assertEquals("gemma3:4b", response.model());
        verify(clyvoAiClient).chat(any(ClyvoAiRequestDTO.class));
    }

    @Test
    void propagatesControlledUnavailableError() {
        stubAuthorizedEmptyContext();
        when(clyvoAiClient.chat(any())).thenThrow(new CustomException("indisponível", HttpStatus.SERVICE_UNAVAILABLE));

        CustomException error = assertThrows(CustomException.class,
                () -> service.askTutor(tutor.getCpf(), animal.getId(), "Pergunta"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
    }

    @Test
    void propagatesControlledTimeoutError() {
        stubAuthorizedEmptyContext();
        when(clyvoAiClient.chat(any())).thenThrow(new CustomException("timeout", HttpStatus.GATEWAY_TIMEOUT));

        CustomException error = assertThrows(CustomException.class,
                () -> service.askTutor(tutor.getCpf(), animal.getId(), "Pergunta"));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, error.getStatus());
    }

    private void stubAuthorizedEmptyContext() {
        when(tutorRepository.findByCpf(tutor.getCpf())).thenReturn(Optional.of(tutor));
        when(animalRepository.findById(animal.getId())).thenReturn(Optional.of(animal));
        lenient().when(medicalRecordRepository.findByAnimalIdOrderByCreatedAtDesc(eq(animal.getId()), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(examRepository.findByMedicalRecordAnimalIdOrderByRequestDateDesc(eq(animal.getId()), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(vaccinationRepository.findByAnimalIdOrderByApplicationDateDesc(eq(animal.getId()), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(appointmentRepository.findTop3ByAnimalIdAndScheduledAtGreaterThanEqualAndStatusInOrderByScheduledAtAsc(
                eq(animal.getId()), any(LocalDateTime.class), anyCollection())).thenReturn(List.of());
        lenient().when(appointmentRepository.findTop5ByAnimalIdAndScheduledAtLessThanAndStatusOrderByScheduledAtDesc(
                eq(animal.getId()), any(LocalDateTime.class), eq(AppointmentStatus.COMPLETED))).thenReturn(List.of());
        when(clyvoAiClient.chat(any())).thenReturn(new ClyvoAiResponseDTO("Resposta", "Aviso", "gemma3:4b"));
    }

    private ClyvoAiRequestDTO capturedRequest() {
        ArgumentCaptor<ClyvoAiRequestDTO> captor = ArgumentCaptor.forClass(ClyvoAiRequestDTO.class);
        verify(clyvoAiClient).chat(captor.capture());
        return captor.getValue();
    }

    private MedicalRecord record(Long id) {
        MedicalRecord record = new MedicalRecord();
        record.setId(id);
        record.setAnimal(animal);
        record.setVeterinarian(veterinarian);
        record.setDiagnosis("Diagnóstico " + id);
        record.setDescription("Descrição");
        record.setWeight(9.5f);
        record.setTemperature(38.5f);
        record.setObservations("Observação");
        record.setCreatedAt(LocalDateTime.now().minusDays(id));
        return record;
    }

    private Exam exam(Long id) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setExamName("Hemograma " + id);
        exam.setExamType("Laboratorial");
        exam.setStatus(ExamStatus.COMPLETED);
        exam.setRequestDate(LocalDateTime.now().minusDays(id));
        exam.setResultDate(LocalDateTime.now());
        exam.setResult("Normal");
        return exam;
    }

    private Vaccination vaccination(Long id) {
        Vaccination vaccination = new Vaccination();
        vaccination.setId(id);
        vaccination.setVaccineName("V" + id);
        vaccination.setApplicationDate(LocalDate.now().minusMonths(id));
        vaccination.setManufacturer("Fabricante");
        return vaccination;
    }

    private Appointment appointment(Long id, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setScheduledAt(LocalDateTime.now().plusDays(id));
        appointment.setReason("Consulta " + id);
        appointment.setStatus(status);
        appointment.setVeterinarian(veterinarian);
        return appointment;
    }

    private Prescription prescription(Long id, MedicalRecord record) {
        Prescription prescription = new Prescription();
        prescription.setId(id);
        prescription.setMedicalRecord(record);
        prescription.setCreatedAt(LocalDateTime.now());
        PrescriptionItem item = new PrescriptionItem();
        item.setMedicationName("Amoxicilina");
        item.setDosage("10 mg");
        item.setFrequency("12/12h");
        item.setDuration("7 dias");
        item.setRoute("oral");
        item.setInstructions("Após alimentação");
        item.setPrescription(prescription);
        prescription.setItems(new ArrayList<>(List.of(item)));
        return prescription;
    }
}
