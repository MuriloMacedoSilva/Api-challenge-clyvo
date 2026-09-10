package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.VeterinarianDashboardResponseDTO;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Appointment;
import com.FirstApiChallenge.api.model.Exam;
import com.FirstApiChallenge.api.model.MedicalRecord;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.AppointmentRepository;
import com.FirstApiChallenge.api.repository.ExamRepository;
import com.FirstApiChallenge.api.repository.MedicalRecordRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VeterinarianDashboardServiceTests {

    @Autowired
    private VeterinarianDashboardService dashboardService;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private VeterinarianTutorLinkRepository linkRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private MockMvc mockMvc;

    private Veterinarian veterinarian;

    @BeforeEach
    void setUp() {
        veterinarian = veterinarianRepository.save(createVeterinarian(
                "10987654321",
                "Vet Principal",
                "1234"
        ));
    }

    @Test
    void returnsNotFoundForUnknownVeterinarian() throws Exception {
        mockMvc.perform(get("/v1/dashboards/veterinarian/00000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinário não encontrado."));
    }

    @Test
    void returnsEmptyDashboardForVeterinarianWithoutData() {
        VeterinarianDashboardResponseDTO dashboard = dashboardService.getDashboard(veterinarian.getCpf());

        assertEquals(0, dashboard.linkedTutors());
        assertEquals(0, dashboard.activePatients());
        assertEquals(0, dashboard.appointmentsToday());
        assertEquals(0, dashboard.pendingAppointments());
        assertEquals(0, dashboard.confirmedAppointments());
        assertEquals(0, dashboard.completedAppointments());
        assertEquals(0, dashboard.pendingExams());
        assertTrue(dashboard.upcomingAppointments().isEmpty());
        assertTrue(dashboard.patientsBySpecies().isEmpty());
        assertEquals(6, dashboard.appointmentsByMonth().size());
        assertTrue(dashboard.appointmentsByMonth().stream().allMatch(metric -> metric.count() == 0));
    }

    @Test
    void countsOnlyAcceptedTutorLinks() {
        createLink(createTutor("11111111111", "Tutor A"), LinkStatus.ACCEPTED);
        createLink(createTutor("22222222222", "Tutor B"), LinkStatus.ACCEPTED);
        createLink(createTutor("33333333333", "Tutor C"), LinkStatus.PENDING);
        createLink(createTutor("44444444444", "Tutor D"), LinkStatus.REJECTED);

        assertEquals(2, dashboardService.getDashboard(veterinarian.getCpf()).linkedTutors());
    }

    @Test
    void countsPatientsOnlyFromAcceptedTutors() {
        Tutor acceptedA = createTutor("11111111111", "Tutor A");
        Tutor acceptedB = createTutor("22222222222", "Tutor B");
        Tutor pending = createTutor("33333333333", "Tutor C");
        createLink(acceptedA, LinkStatus.ACCEPTED);
        createLink(acceptedB, LinkStatus.ACCEPTED);
        createLink(pending, LinkStatus.PENDING);
        createAnimals(acceptedA, 2, "Cachorro");
        createAnimals(acceptedB, 3, "Gato");
        createAnimals(pending, 4, "Ave");

        assertEquals(5, dashboardService.getDashboard(veterinarian.getCpf()).activePatients());
    }

    @Test
    void countsAllTodayAppointmentsExceptCancelled() {
        Tutor tutor = acceptedTutor("11111111111", "Tutor A");
        Animal animal = createAnimal(tutor, "Luna", "Cachorro");
        LocalDateTime today = LocalDateTime.now().toLocalDate().atTime(10, 0);
        createAppointment(veterinarian, tutor, animal, today, AppointmentStatus.PENDING);
        createAppointment(veterinarian, tutor, animal, today.plusHours(1), AppointmentStatus.CONFIRMED);
        createAppointment(veterinarian, tutor, animal, today.plusHours(2), AppointmentStatus.COMPLETED);
        createAppointment(veterinarian, tutor, animal, today.plusHours(3), AppointmentStatus.CANCELLED);
        createAppointment(veterinarian, tutor, animal, today.plusDays(1), AppointmentStatus.CONFIRMED);

        assertEquals(3, dashboardService.getDashboard(veterinarian.getCpf()).appointmentsToday());
    }

    @Test
    void countsAppointmentStatusesIndependently() {
        Tutor tutor = acceptedTutor("11111111111", "Tutor A");
        Animal animal = createAnimal(tutor, "Luna", "Cachorro");
        LocalDateTime base = LocalDateTime.now().plusDays(2);
        createAppointment(veterinarian, tutor, animal, base, AppointmentStatus.PENDING);
        createAppointment(veterinarian, tutor, animal, base.plusHours(1), AppointmentStatus.PENDING);
        createAppointment(veterinarian, tutor, animal, base.plusHours(2), AppointmentStatus.CONFIRMED);
        createAppointment(veterinarian, tutor, animal, base.minusMonths(1), AppointmentStatus.COMPLETED);
        createAppointment(veterinarian, tutor, animal, base.plusHours(3), AppointmentStatus.CANCELLED);

        VeterinarianDashboardResponseDTO dashboard = dashboardService.getDashboard(veterinarian.getCpf());
        assertEquals(2, dashboard.pendingAppointments());
        assertEquals(1, dashboard.confirmedAppointments());
        assertEquals(1, dashboard.completedAppointments());
    }

    @Test
    void doesNotCountAppointmentsFromAnotherVeterinarian() {
        Tutor tutor = acceptedTutor("11111111111", "Tutor A");
        Animal animal = createAnimal(tutor, "Luna", "Cachorro");
        Veterinarian other = veterinarianRepository.save(createVeterinarian(
                "12312312312",
                "Vet B",
                "5678"
        ));
        LocalDateTime today = LocalDateTime.now().plusHours(1);
        createAppointment(veterinarian, tutor, animal, today, AppointmentStatus.PENDING);
        createAppointment(other, tutor, animal, today.plusHours(1), AppointmentStatus.CONFIRMED);

        VeterinarianDashboardResponseDTO dashboard = dashboardService.getDashboard(veterinarian.getCpf());
        assertEquals(1, dashboard.pendingAppointments());
        assertEquals(0, dashboard.confirmedAppointments());
    }

    @Test
    void countsOnlyRequestedExamsAuthoredByVeterinarian() {
        Tutor tutor = acceptedTutor("11111111111", "Tutor A");
        Animal animal = createAnimal(tutor, "Luna", "Cachorro");
        Veterinarian other = veterinarianRepository.save(createVeterinarian(
                "12312312312",
                "Vet B",
                "5678"
        ));
        MedicalRecord ownRecord = createMedicalRecord(veterinarian, tutor, animal, 1);
        MedicalRecord otherRecord = createMedicalRecord(other, tutor, animal, 2);
        createExam(ownRecord, ExamStatus.REQUESTED);
        createExam(ownRecord, ExamStatus.COMPLETED);
        createExam(ownRecord, ExamStatus.CANCELLED);
        createExam(otherRecord, ExamStatus.REQUESTED);

        assertEquals(1, dashboardService.getDashboard(veterinarian.getCpf()).pendingExams());
    }

    @Test
    void aggregatesPatientsByExistingSpeciesValue() {
        Tutor tutor = acceptedTutor("11111111111", "Tutor A");
        createAnimals(tutor, 3, "Cachorro");
        createAnimals(tutor, 2, "Gato");
        createAnimals(tutor, 1, "Outro");

        var metrics = dashboardService.getDashboard(veterinarian.getCpf()).patientsBySpecies();
        assertEquals(List.of("Cachorro", "Gato", "Outro"), metrics.stream().map(metric -> metric.species()).toList());
        assertEquals(List.of(3L, 2L, 1L), metrics.stream().map(metric -> metric.count()).toList());
    }

    @Test
    void returnsContinuousSixMonthSeriesWithOnlyCompletedOwnAppointments() {
        Tutor tutor = acceptedTutor("11111111111", "Tutor A");
        Animal animal = createAnimal(tutor, "Luna", "Cachorro");
        Veterinarian other = veterinarianRepository.save(createVeterinarian(
                "12312312312",
                "Vet B",
                "5678"
        ));
        YearMonth current = YearMonth.now();
        createAppointment(veterinarian, tutor, animal, current.minusMonths(5).atDay(10).atTime(9, 0), AppointmentStatus.COMPLETED);
        createAppointment(veterinarian, tutor, animal, current.minusMonths(2).atDay(10).atTime(9, 0), AppointmentStatus.COMPLETED);
        createAppointment(veterinarian, tutor, animal, current.minusMonths(2).atDay(11).atTime(9, 0), AppointmentStatus.PENDING);
        createAppointment(other, tutor, animal, current.minusMonths(1).atDay(10).atTime(9, 0), AppointmentStatus.COMPLETED);

        var metrics = dashboardService.getDashboard(veterinarian.getCpf()).appointmentsByMonth();
        assertEquals(6, metrics.size());
        assertEquals(List.of(1L, 0L, 0L, 1L, 0L, 0L), metrics.stream().map(metric -> metric.count()).toList());
        assertEquals(current.minusMonths(5).getYear(), metrics.getFirst().year());
        assertEquals(current.getMonthValue(), metrics.getLast().month());
    }

    @Test
    void buildsSixMonthsAcrossYearBoundary() {
        List<YearMonth> months = VeterinarianDashboardService.lastSixMonths(YearMonth.of(2026, 1));

        assertEquals(List.of(
                YearMonth.of(2025, 8),
                YearMonth.of(2025, 9),
                YearMonth.of(2025, 10),
                YearMonth.of(2025, 11),
                YearMonth.of(2025, 12),
                YearMonth.of(2026, 1)
        ), months);
    }

    @Test
    void returnsOnlyFiveUpcomingActiveAppointmentsInChronologicalOrder() {
        Tutor tutor = acceptedTutor("11111111111", "Tutor A");
        Animal animal = createAnimal(tutor, "Luna", "Cachorro");
        LocalDateTime base = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        createAppointment(veterinarian, tutor, animal, base.minusDays(2), AppointmentStatus.PENDING);
        createAppointment(veterinarian, tutor, animal, base, AppointmentStatus.CANCELLED);
        for (int index = 1; index <= 6; index++) {
            createAppointment(
                    veterinarian,
                    tutor,
                    animal,
                    base.plusHours(index),
                    index % 2 == 0 ? AppointmentStatus.CONFIRMED : AppointmentStatus.PENDING
            );
        }

        var upcoming = dashboardService.getDashboard(veterinarian.getCpf()).upcomingAppointments();
        assertEquals(5, upcoming.size());
        assertTrue(upcoming.stream().allMatch(item -> item.status() != AppointmentStatus.CANCELLED));
        assertTrue(upcoming.get(0).scheduledAt().isBefore(upcoming.get(4).scheduledAt()));
        assertEquals(base.plusHours(1), upcoming.getFirst().scheduledAt());
        assertEquals(base.plusHours(5), upcoming.getLast().scheduledAt());
    }

    @Test
    void separatesSharedPortfolioFromClinicalAuthorship() {
        Tutor tutor = createTutor("11111111111", "Tutor A");
        Animal animal = createAnimal(tutor, "Luna", "Cachorro");
        createLink(tutor, LinkStatus.ACCEPTED);
        Veterinarian other = veterinarianRepository.save(createVeterinarian(
                "12312312312",
                "Vet B",
                "5678"
        ));
        VeterinarianTutorLink otherLink = new VeterinarianTutorLink(other, tutor);
        otherLink.setStatus(LinkStatus.ACCEPTED);
        linkRepository.save(otherLink);
        createAppointment(veterinarian, tutor, animal, LocalDateTime.now().plusDays(1), AppointmentStatus.CONFIRMED);
        MedicalRecord ownRecord = createMedicalRecord(veterinarian, tutor, animal, 1);
        createExam(ownRecord, ExamStatus.REQUESTED);

        VeterinarianDashboardResponseDTO otherDashboard = dashboardService.getDashboard(other.getCpf());
        assertEquals(1, otherDashboard.linkedTutors());
        assertEquals(1, otherDashboard.activePatients());
        assertEquals(0, otherDashboard.confirmedAppointments());
        assertEquals(0, otherDashboard.pendingExams());
    }

    private Tutor acceptedTutor(String cpf, String name) {
        Tutor tutor = createTutor(cpf, name);
        createLink(tutor, LinkStatus.ACCEPTED);
        return tutor;
    }

    private Tutor createTutor(String cpf, String name) {
        Tutor tutor = new Tutor();
        tutor.setName(name);
        tutor.setEmail(cpf + "@email.com");
        tutor.setCpf(cpf);
        tutor.setPhoneNumber("11999999999");
        tutor.setPassword("12345678");
        tutor.setRole("tutor");
        return tutorRepository.save(tutor);
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

    private void createLink(Tutor tutor, LinkStatus status) {
        VeterinarianTutorLink link = new VeterinarianTutorLink(veterinarian, tutor);
        link.setStatus(status);
        linkRepository.save(link);
    }

    private void createAnimals(Tutor tutor, int count, String species) {
        for (int index = 1; index <= count; index++) {
            createAnimal(tutor, species + " " + index + " " + tutor.getCpf(), species);
        }
    }

    private Animal createAnimal(Tutor tutor, String name, String species) {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setWeight(8.5f);
        animal.setHeight(0.4f);
        animal.setAge(3);
        animal.setRace("SRD");
        animal.setSpecies(species);
        animal.setHistory("");
        animal.setTutor(tutor);
        return animalRepository.save(animal);
    }

    private Appointment createAppointment(
            Veterinarian responsible,
            Tutor tutor,
            Animal animal,
            LocalDateTime scheduledAt,
            AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setVeterinarian(responsible);
        appointment.setTutor(tutor);
        appointment.setAnimal(animal);
        appointment.setScheduledAt(scheduledAt);
        appointment.setReason("Consulta de teste");
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    private MedicalRecord createMedicalRecord(
            Veterinarian responsible,
            Tutor tutor,
            Animal animal,
            int sequence) {
        Appointment appointment = createAppointment(
                responsible,
                tutor,
                animal,
                LocalDateTime.now().minusDays(sequence),
                AppointmentStatus.COMPLETED
        );
        MedicalRecord record = new MedicalRecord();
        record.setVeterinarian(responsible);
        record.setAnimal(animal);
        record.setAppointment(appointment);
        record.setDiagnosis("Diagnóstico");
        record.setDescription("Descrição clínica");
        return medicalRecordRepository.save(record);
    }

    private void createExam(MedicalRecord medicalRecord, ExamStatus status) {
        Exam exam = new Exam();
        exam.setMedicalRecord(medicalRecord);
        exam.setExamName("Hemograma");
        exam.setStatus(status);
        exam.setRequestDate(LocalDateTime.now().minusDays(1));
        if (status == ExamStatus.COMPLETED) {
            exam.setResult("Resultado");
            exam.setResultDate(LocalDateTime.now());
        }
        examRepository.save(exam);
    }
}
