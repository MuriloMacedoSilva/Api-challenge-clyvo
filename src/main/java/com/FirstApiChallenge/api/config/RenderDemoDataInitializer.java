package com.FirstApiChallenge.api.config;

import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.MessageSenderType;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Appointment;
import com.FirstApiChallenge.api.model.Conversation;
import com.FirstApiChallenge.api.model.Exam;
import com.FirstApiChallenge.api.model.MedicalRecord;
import com.FirstApiChallenge.api.model.Message;
import com.FirstApiChallenge.api.model.Prescription;
import com.FirstApiChallenge.api.model.PrescriptionItem;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Vaccination;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.AppointmentRepository;
import com.FirstApiChallenge.api.repository.ConversationRepository;
import com.FirstApiChallenge.api.repository.ExamRepository;
import com.FirstApiChallenge.api.repository.MedicalRecordRepository;
import com.FirstApiChallenge.api.repository.MessageRepository;
import com.FirstApiChallenge.api.repository.PrescriptionRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VaccinationRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("render")
public class RenderDemoDataInitializer implements CommandLineRunner {

    public static final String TUTOR_CPF = "12345678901";
    public static final String VETERINARIAN_CPF = "98765432100";
    public static final String VETERINARIAN_CRMV = "12345";
    public static final String DEMO_PASSWORD = "12345678";

    private static final Logger LOGGER = LoggerFactory.getLogger(RenderDemoDataInitializer.class);

    private final TutorRepository tutorRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final AnimalRepository animalRepository;
    private final VeterinarianTutorLinkRepository linkRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ExamRepository examRepository;
    private final VaccinationRepository vaccinationRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public RenderDemoDataInitializer(
            TutorRepository tutorRepository,
            VeterinarianRepository veterinarianRepository,
            AnimalRepository animalRepository,
            VeterinarianTutorLinkRepository linkRepository,
            AppointmentRepository appointmentRepository,
            MedicalRecordRepository medicalRecordRepository,
            PrescriptionRepository prescriptionRepository,
            ExamRepository examRepository,
            VaccinationRepository vaccinationRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository) {
        this.tutorRepository = tutorRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.animalRepository = animalRepository;
        this.linkRepository = linkRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.examRepository = examRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (tutorRepository.findByCpf(TUTOR_CPF).isPresent()) {
            LOGGER.info("Massa demo do profile render ja existe; inicializacao ignorada.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Tutor tutor = createTutor();
        Veterinarian veterinarian = createVeterinarian();
        tutorRepository.saveAndFlush(tutor);
        veterinarianRepository.saveAndFlush(veterinarian);

        Animal luna = createAnimal(tutor, "Luna", 29.4F, 0.58F, 5,
                "Golden Retriever", "Cachorro", "Acompanhamento gastrointestinal e vacinal.");
        Animal mingau = createAnimal(tutor, "Mingau", 5.2F, 0.28F, 4,
                "SRD", "Gato", "Animal ativo, em acompanhamento preventivo.");
        Animal thor = createAnimal(tutor, "Thor", 12.8F, 0.34F, 3,
                "Bulldog Frances", "Cachorro", "Acompanhamento respiratorio preventivo.");
        animalRepository.saveAllAndFlush(List.of(luna, mingau, thor));

        VeterinarianTutorLink link = new VeterinarianTutorLink(veterinarian, tutor);
        link.setStatus(LinkStatus.ACCEPTED);
        link.setCreatedAt(now.minusMonths(6));
        link.setUpdatedAt(now.minusMonths(6));
        linkRepository.saveAndFlush(link);

        Appointment completedAppointment = createAppointment(
                luna, tutor, veterinarian, now.minusDays(14),
                "Desconforto gastrointestinal e reducao de apetite.", AppointmentStatus.COMPLETED);
        Appointment confirmedAppointment = createAppointment(
                mingau, tutor, veterinarian, futureTime(now, 1, 10),
                "Avaliacao preventiva anual.", AppointmentStatus.CONFIRMED);
        Appointment pendingAppointment = createAppointment(
                thor, tutor, veterinarian, futureTime(now, 3, 15),
                "Acompanhamento respiratorio.", AppointmentStatus.PENDING);
        appointmentRepository.saveAllAndFlush(List.of(
                completedAppointment, confirmedAppointment, pendingAppointment));

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setAnimal(luna);
        medicalRecord.setVeterinarian(veterinarian);
        medicalRecord.setAppointment(completedAppointment);
        medicalRecord.setDiagnosis("Quadro gastrointestinal leve");
        medicalRecord.setDescription("Exame clinico sem sinais de gravidade, com hidratacao preservada.");
        medicalRecord.setWeight(29.4F);
        medicalRecord.setTemperature(38.6F);
        medicalRecord.setObservations("Retorno recomendado se os sintomas persistirem.");
        medicalRecordRepository.saveAndFlush(medicalRecord);

        Prescription prescription = new Prescription();
        prescription.setMedicalRecord(medicalRecord);
        prescription.setInstructions("Administrar apos alimentacao e manter agua disponivel.");
        prescription.getItems().add(createPrescriptionItem(
                prescription, "Probiotico veterinario", "1 sache", "Uma vez ao dia",
                "7 dias", "Oral", "Misturar ao alimento."));
        prescription.getItems().add(createPrescriptionItem(
                prescription, "Protetor gastrico demonstrativo", "1 comprimido", "A cada 12 horas",
                "5 dias", "Oral", "Administrar conforme orientacao veterinaria."));
        prescriptionRepository.saveAndFlush(prescription);

        Exam completedExam = createExam(medicalRecord, "Hemograma completo", "Laboratorial",
                ExamStatus.COMPLETED, now.minusDays(13), now.minusDays(12),
                "Parametros dentro dos valores esperados.", "Sem alteracoes relevantes.");
        Exam requestedExam = createExam(medicalRecord, "Ultrassonografia abdominal", "Imagem",
                ExamStatus.REQUESTED, now.minusDays(1), null, null,
                "Exame de controle solicitado.");
        examRepository.saveAllAndFlush(List.of(completedExam, requestedExam));

        vaccinationRepository.saveAllAndFlush(List.of(
                createVaccination(luna, veterinarian, "Antirrabica", today.minusDays(60),
                        today.plusDays(305), "LUNA-AR-001", "Laboratorio Demo"),
                createVaccination(mingau, veterinarian, "Triplice Felina", today.minusDays(30),
                        today.plusDays(335), "MINGAU-V3-001", "Laboratorio Demo"),
                createVaccination(thor, veterinarian, "V10", today.minusDays(90),
                        today.plusDays(275), "THOR-V10-001", "Laboratorio Demo")));

        Conversation conversation = new Conversation();
        conversation.setTutor(tutor);
        conversation.setVeterinarian(veterinarian);
        conversationRepository.saveAndFlush(conversation);

        Message tutorMessage = createMessage(conversation, MessageSenderType.TUTOR,
                "Ola, doutor. A Luna esta melhor depois da consulta.", now.minusDays(2), true);
        Message veterinarianMessage = createMessage(conversation, MessageSenderType.VETERINARIAN,
                "Que bom! Continue seguindo a prescricao e observe o apetite.", now.minusDays(2).plusMinutes(10), true);
        Message followUp = createMessage(conversation, MessageSenderType.TUTOR,
                "Combinado. Tambem vi a solicitacao do exame de controle.", now.minusHours(3), false);
        messageRepository.saveAllAndFlush(List.of(tutorMessage, veterinarianMessage, followUp));

        LOGGER.info("Massa demo do Render criada: 1 Tutor, 1 Veterinario, 3 Animals, 3 Appointments e dados clinicos/chat.");
    }

    private Tutor createTutor() {
        Tutor tutor = new Tutor();
        tutor.setName("Mariana Oliveira");
        tutor.setEmail("mariana.oliveira@demo.clyvo.com");
        tutor.setCpf(TUTOR_CPF);
        tutor.setPhoneNumber("11987654321");
        tutor.setPassword(DEMO_PASSWORD);
        tutor.setRole("tutor");
        return tutor;
    }

    private Veterinarian createVeterinarian() {
        Veterinarian veterinarian = new Veterinarian();
        veterinarian.setName("Dr. Gabriel Martins");
        veterinarian.setEmail("gabriel.martins@demo.clyvo.com");
        veterinarian.setCpf(VETERINARIAN_CPF);
        veterinarian.setPhoneNumber("11999887766");
        veterinarian.setPassword(DEMO_PASSWORD);
        veterinarian.setRole("veterinarian");
        veterinarian.setCrmvNumber(VETERINARIAN_CRMV);
        veterinarian.setCrmvState("SP");
        veterinarian.setCnpj("12345678000190");
        return veterinarian;
    }

    private Animal createAnimal(Tutor tutor, String name, Float weight, Float height, Integer age,
                                String race, String species, String history) {
        Animal animal = new Animal();
        animal.setTutor(tutor);
        animal.setName(name);
        animal.setWeight(weight);
        animal.setHeight(height);
        animal.setAge(age);
        animal.setRace(race);
        animal.setSpecies(species);
        animal.setHistory(history);
        return animal;
    }

    private Appointment createAppointment(Animal animal, Tutor tutor, Veterinarian veterinarian,
                                          LocalDateTime scheduledAt, String reason,
                                          AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setAnimal(animal);
        appointment.setTutor(tutor);
        appointment.setVeterinarian(veterinarian);
        appointment.setScheduledAt(scheduledAt);
        appointment.setReason(reason);
        appointment.setStatus(status);
        return appointment;
    }

    private PrescriptionItem createPrescriptionItem(Prescription prescription, String medicationName,
                                                    String dosage, String frequency, String duration,
                                                    String route, String instructions) {
        PrescriptionItem item = new PrescriptionItem();
        item.setPrescription(prescription);
        item.setMedicationName(medicationName);
        item.setDosage(dosage);
        item.setFrequency(frequency);
        item.setDuration(duration);
        item.setRoute(route);
        item.setInstructions(instructions);
        return item;
    }

    private Exam createExam(MedicalRecord medicalRecord, String name, String type, ExamStatus status,
                            LocalDateTime requestDate, LocalDateTime resultDate, String result,
                            String observations) {
        Exam exam = new Exam();
        exam.setMedicalRecord(medicalRecord);
        exam.setExamName(name);
        exam.setExamType(type);
        exam.setStatus(status);
        exam.setRequestDate(requestDate);
        exam.setResultDate(resultDate);
        exam.setResult(result);
        exam.setObservations(observations);
        return exam;
    }

    private Vaccination createVaccination(Animal animal, Veterinarian veterinarian, String name,
                                          LocalDate applicationDate, LocalDate nextDoseDate,
                                          String batchNumber, String manufacturer) {
        Vaccination vaccination = new Vaccination();
        vaccination.setAnimal(animal);
        vaccination.setVeterinarian(veterinarian);
        vaccination.setVaccineName(name);
        vaccination.setApplicationDate(applicationDate);
        vaccination.setNextDoseDate(nextDoseDate);
        vaccination.setBatchNumber(batchNumber);
        vaccination.setManufacturer(manufacturer);
        vaccination.setObservations("Registro ficticio para demonstracao do frontend.");
        return vaccination;
    }

    private Message createMessage(Conversation conversation, MessageSenderType senderType,
                                  String content, LocalDateTime sentAt, boolean read) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setSentAt(sentAt);
        message.setRead(read);
        if (read) {
            message.setReadAt(sentAt.plusMinutes(2));
        }
        return message;
    }

    private LocalDateTime futureTime(LocalDateTime now, long days, int hour) {
        return now.plusDays(days).withHour(hour).withMinute(0).withSecond(0);
    }
}
