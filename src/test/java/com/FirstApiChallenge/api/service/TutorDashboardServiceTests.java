package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.TutorDashboardResponseDTO;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Appointment;
import com.FirstApiChallenge.api.model.Exam;
import com.FirstApiChallenge.api.model.MedicalRecord;
import com.FirstApiChallenge.api.model.Prescription;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Vaccination;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.AppointmentRepository;
import com.FirstApiChallenge.api.repository.ExamRepository;
import com.FirstApiChallenge.api.repository.MedicalRecordRepository;
import com.FirstApiChallenge.api.repository.PrescriptionRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VaccinationRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TutorDashboardServiceTests {

    @Autowired
    private TutorDashboardService dashboardService;

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
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private VaccinationRepository vaccinationRepository;

    @Autowired
    private MockMvc mockMvc;

    private Tutor tutor;
    private Veterinarian veterinarian;

    @BeforeEach
    void setUp() {
        tutor = createTutor("11111111111", "Tutor Principal");
        veterinarian = createVeterinarian("99999999999", "Dra. Ana", "1234");
    }

    @Test
    void returnsNotFoundForUnknownTutor() throws Exception {
        mockMvc.perform(get("/v1/dashboards/tutor/00000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tutor não encontrado."));
    }

    @Test
    void returnsEmptyDashboardForTutorWithoutData() {
        TutorDashboardResponseDTO dashboard = dashboardService.getDashboard(tutor.getCpf());

        assertEquals(0, dashboard.totalPets());
        assertEquals(0, dashboard.appointmentsToday());
        assertEquals(0, dashboard.pendingAppointments());
        assertEquals(0, dashboard.linkedVeterinarians());
        assertEquals(0, dashboard.pendingExams());
        assertEquals(0, dashboard.vaccinationsAttention());
        assertNull(dashboard.nextAppointment());
        assertTrue(dashboard.recentActivities().isEmpty());
    }

    @Test
    void countsOnlyAnimalsOwnedByTutor() {
        Tutor otherTutor = createTutor("22222222222", "Outro Tutor");
        createAnimal(tutor, "Luna");
        createAnimal(tutor, "Thor");
        createAnimal(otherTutor, "Mingau");

        assertEquals(2, dashboardService.getDashboard(tutor.getCpf()).totalPets());
    }

    @Test
    void appointmentsTodayIgnoresCancelled() {
        Animal animal = createAnimal(tutor, "Luna");
        LocalDateTime today = LocalDate.now().atTime(10, 0);
        createAppointment(tutor, animal, today, AppointmentStatus.PENDING);
        createAppointment(tutor, animal, today.plusHours(1), AppointmentStatus.CONFIRMED);
        createAppointment(tutor, animal, today.plusHours(2), AppointmentStatus.COMPLETED);
        createAppointment(tutor, animal, today.plusHours(3), AppointmentStatus.CANCELLED);
        createAppointment(tutor, animal, today.plusDays(1), AppointmentStatus.PENDING);

        assertEquals(3, dashboardService.getDashboard(tutor.getCpf()).appointmentsToday());
    }

    @Test
    void appointmentsTodayDoesNotIncludeAnotherTutor() {
        Tutor otherTutor = createTutor("22222222222", "Outro Tutor");
        Animal ownAnimal = createAnimal(tutor, "Luna");
        Animal otherAnimal = createAnimal(otherTutor, "Mingau");
        LocalDateTime today = LocalDate.now().atTime(10, 0);
        createAppointment(tutor, ownAnimal, today, AppointmentStatus.PENDING);
        createAppointment(otherTutor, otherAnimal, today.plusHours(1), AppointmentStatus.CONFIRMED);

        assertEquals(1, dashboardService.getDashboard(tutor.getCpf()).appointmentsToday());
    }

    @Test
    void pendingAppointmentsCountsOnlyPendingStatus() {
        Animal animal = createAnimal(tutor, "Luna");
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        createAppointment(tutor, animal, future, AppointmentStatus.PENDING);
        createAppointment(tutor, animal, future.plusHours(1), AppointmentStatus.PENDING);
        createAppointment(tutor, animal, future.plusHours(2), AppointmentStatus.CONFIRMED);
        createAppointment(tutor, animal, future.plusHours(3), AppointmentStatus.COMPLETED);

        assertEquals(2, dashboardService.getDashboard(tutor.getCpf()).pendingAppointments());
    }

    @Test
    void linkedVeterinariansCountsOnlyAcceptedLinks() {
        createLink(tutor, veterinarian, LinkStatus.ACCEPTED);
        createLink(tutor, createVeterinarian("88888888888", "Dr. B", "2345"), LinkStatus.PENDING);
        createLink(tutor, createVeterinarian("77777777777", "Dr. C", "3456"), LinkStatus.REJECTED);

        assertEquals(1, dashboardService.getDashboard(tutor.getCpf()).linkedVeterinarians());
    }

    @Test
    void pendingExamsCountsOnlyRequestedForTutorAnimals() {
        Animal animal = createAnimal(tutor, "Luna");
        MedicalRecord record = createMedicalRecord(tutor, animal, 1);
        createExam(record, ExamStatus.REQUESTED, null);
        createExam(record, ExamStatus.COMPLETED, LocalDateTime.now());
        createExam(record, ExamStatus.CANCELLED, null);

        assertEquals(1, dashboardService.getDashboard(tutor.getCpf()).pendingExams());
    }

    @Test
    void pendingExamsDoesNotLeakFromAnotherTutor() {
        Tutor otherTutor = createTutor("22222222222", "Outro Tutor");
        Animal ownAnimal = createAnimal(tutor, "Luna");
        Animal otherAnimal = createAnimal(otherTutor, "Mingau");
        createExam(createMedicalRecord(tutor, ownAnimal, 1), ExamStatus.REQUESTED, null);
        createExam(createMedicalRecord(otherTutor, otherAnimal, 2), ExamStatus.REQUESTED, null);

        assertEquals(1, dashboardService.getDashboard(tutor.getCpf()).pendingExams());
    }

    @Test
    void vaccinationsAttentionIncludesTodayAndPastDates() {
        Animal animal = createAnimal(tutor, "Luna");
        createVaccination(animal, "V10", LocalDate.now());
        createVaccination(animal, "Antirrábica", LocalDate.now().minusDays(1));

        assertEquals(2, dashboardService.getDashboard(tutor.getCpf()).vaccinationsAttention());
    }

    @Test
    void vaccinationsAttentionIgnoresNullNextDoseDate() {
        createVaccination(createAnimal(tutor, "Luna"), "V10", null);

        assertEquals(0, dashboardService.getDashboard(tutor.getCpf()).vaccinationsAttention());
    }

    @Test
    void vaccinationsAttentionIgnoresFutureDates() {
        createVaccination(createAnimal(tutor, "Luna"), "V10", LocalDate.now().plusDays(1));

        assertEquals(0, dashboardService.getDashboard(tutor.getCpf()).vaccinationsAttention());
    }

    @Test
    void nextAppointmentReturnsNearestActiveAppointment() {
        Animal animal = createAnimal(tutor, "Luna");
        LocalDateTime future = LocalDateTime.now().plusDays(1).withNano(0);
        createAppointment(tutor, animal, future.plusHours(2), AppointmentStatus.PENDING);
        Appointment nearest = createAppointment(tutor, animal, future, AppointmentStatus.CONFIRMED);

        assertEquals(nearest.getId(), dashboardService.getDashboard(tutor.getCpf()).nextAppointment().appointmentId());
    }

    @Test
    void nextAppointmentIgnoresCancelledCompletedAndPastAppointments() {
        Animal animal = createAnimal(tutor, "Luna");
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        createAppointment(tutor, animal, future, AppointmentStatus.CANCELLED);
        createAppointment(tutor, animal, future.plusHours(1), AppointmentStatus.COMPLETED);
        createAppointment(tutor, animal, LocalDateTime.now().minusDays(1), AppointmentStatus.PENDING);

        assertNull(dashboardService.getDashboard(tutor.getCpf()).nextAppointment());
    }

    @Test
    void nextAppointmentDoesNotIncludeAnotherTutor() {
        Tutor otherTutor = createTutor("22222222222", "Outro Tutor");
        Animal otherAnimal = createAnimal(otherTutor, "Mingau");
        createAppointment(otherTutor, otherAnimal, LocalDateTime.now().plusDays(1), AppointmentStatus.PENDING);

        assertNull(dashboardService.getDashboard(tutor.getCpf()).nextAppointment());
    }

    @Test
    void recentActivitiesReturnsAtMostFiveItems() {
        Animal animal = createAnimal(tutor, "Luna");
        MedicalRecord record = createMedicalRecord(tutor, animal, 1);
        for (int index = 0; index < 7; index++) {
            createExam(record, ExamStatus.COMPLETED, LocalDateTime.now().plusDays(index + 1));
        }

        assertEquals(5, dashboardService.getDashboard(tutor.getCpf()).recentActivities().size());
    }

    @Test
    void recentActivitiesAreOrderedNewestFirst() {
        Animal animal = createAnimal(tutor, "Luna");
        MedicalRecord record = createMedicalRecord(tutor, animal, 1);
        createExam(record, ExamStatus.COMPLETED, LocalDateTime.now().minusDays(2));
        createExam(record, ExamStatus.COMPLETED, LocalDateTime.now().minusDays(1));
        createExam(record, ExamStatus.COMPLETED, LocalDateTime.now());

        var activities = dashboardService.getDashboard(tutor.getCpf()).recentActivities();
        for (int index = 1; index < activities.size(); index++) {
            assertTrue(!activities.get(index - 1).occurredAt().isBefore(activities.get(index).occurredAt()));
        }
    }

    @Test
    void recentActivitiesDoNotLeakFromAnotherTutor() {
        Tutor otherTutor = createTutor("22222222222", "Outro Tutor");
        Animal ownAnimal = createAnimal(tutor, "Luna");
        Animal otherAnimal = createAnimal(otherTutor, "Mingau");
        createVaccination(ownAnimal, "V10", LocalDate.now().plusMonths(1));
        createVaccination(otherAnimal, "Antirrábica", LocalDate.now().plusMonths(1));

        var activities = dashboardService.getDashboard(tutor.getCpf()).recentActivities();
        assertTrue(activities.stream().allMatch(activity -> activity.animalId().equals(ownAnimal.getId())));
        assertTrue(activities.stream().noneMatch(activity -> activity.animalName().equals("Mingau")));
    }

    @Test
    void recentActivitiesRepresentAllSupportedClinicalEvents() {
        Animal animal = createAnimal(tutor, "Luna");
        Appointment completed = createAppointment(
                tutor,
                animal,
                LocalDateTime.now().minusDays(1),
                AppointmentStatus.COMPLETED
        );
        MedicalRecord record = createMedicalRecord(completed, animal);
        Prescription prescription = new Prescription();
        prescription.setMedicalRecord(record);
        prescriptionRepository.save(prescription);
        createExam(record, ExamStatus.COMPLETED, LocalDateTime.now());
        createVaccination(animal, "V10", LocalDate.now().plusMonths(1));

        List<String> types = dashboardService.getDashboard(tutor.getCpf()).recentActivities().stream()
                .map(activity -> activity.type())
                .toList();
        assertTrue(types.containsAll(List.of(
                "APPOINTMENT_COMPLETED",
                "MEDICAL_RECORD_CREATED",
                "PRESCRIPTION_CREATED",
                "EXAM_RESULT_AVAILABLE",
                "VACCINATION_REGISTERED"
        )));
    }

    private Tutor createTutor(String cpf, String name) {
        Tutor value = new Tutor();
        value.setName(name);
        value.setEmail(cpf + "@email.com");
        value.setCpf(cpf);
        value.setPhoneNumber("11999999999");
        value.setPassword("12345678");
        value.setRole("tutor");
        return tutorRepository.save(value);
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
        value.setCnpj(cpf + "001");
        return veterinarianRepository.save(value);
    }

    private Animal createAnimal(Tutor owner, String name) {
        Animal animal = new Animal();
        animal.setTutor(owner);
        animal.setName(name);
        animal.setWeight(8.5f);
        animal.setHeight(0.4f);
        animal.setAge(3);
        animal.setRace("SRD");
        animal.setSpecies("Cachorro");
        animal.setHistory("");
        return animalRepository.save(animal);
    }

    private void createLink(Tutor owner, Veterinarian vet, LinkStatus status) {
        VeterinarianTutorLink link = new VeterinarianTutorLink(vet, owner);
        link.setStatus(status);
        linkRepository.save(link);
    }

    private Appointment createAppointment(
            Tutor owner,
            Animal animal,
            LocalDateTime scheduledAt,
            AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setTutor(owner);
        appointment.setAnimal(animal);
        appointment.setVeterinarian(veterinarian);
        appointment.setScheduledAt(scheduledAt);
        appointment.setReason("Consulta de teste");
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    private MedicalRecord createMedicalRecord(Tutor owner, Animal animal, int sequence) {
        Appointment appointment = createAppointment(
                owner,
                animal,
                LocalDateTime.now().minusDays(sequence),
                AppointmentStatus.COMPLETED
        );
        return createMedicalRecord(appointment, animal);
    }

    private MedicalRecord createMedicalRecord(Appointment appointment, Animal animal) {
        MedicalRecord record = new MedicalRecord();
        record.setAppointment(appointment);
        record.setAnimal(animal);
        record.setVeterinarian(veterinarian);
        record.setDiagnosis("Diagnóstico");
        record.setDescription("Descrição clínica");
        return medicalRecordRepository.save(record);
    }

    private void createExam(MedicalRecord record, ExamStatus status, LocalDateTime resultDate) {
        Exam exam = new Exam();
        exam.setMedicalRecord(record);
        exam.setExamName("Hemograma");
        exam.setStatus(status);
        exam.setRequestDate(LocalDateTime.now().minusDays(2));
        exam.setResultDate(resultDate);
        if (status == ExamStatus.COMPLETED) {
            exam.setResult("Resultado disponível");
        }
        examRepository.save(exam);
    }

    private void createVaccination(Animal animal, String name, LocalDate nextDoseDate) {
        Vaccination vaccination = new Vaccination();
        vaccination.setAnimal(animal);
        vaccination.setVeterinarian(veterinarian);
        vaccination.setVaccineName(name);
        vaccination.setApplicationDate(LocalDate.now().minusMonths(1));
        vaccination.setNextDoseDate(nextDoseDate);
        vaccinationRepository.save(vaccination);
    }
}
