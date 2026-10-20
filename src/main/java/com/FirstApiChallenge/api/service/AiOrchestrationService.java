package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.client.ClyvoAiClient;
import com.FirstApiChallenge.api.dto.*;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.*;
import com.FirstApiChallenge.api.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AiOrchestrationService {

    static final int MEDICAL_RECORD_LIMIT = 5;
    static final int EXAM_LIMIT = 5;
    static final int VACCINATION_LIMIT = 10;
    static final int APPOINTMENT_LIMIT = 5;

    private final TutorRepository tutorRepository;
    private final AnimalRepository animalRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ExamRepository examRepository;
    private final VaccinationRepository vaccinationRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClyvoAiClient clyvoAiClient;

    public AiOrchestrationService(
            TutorRepository tutorRepository,
            AnimalRepository animalRepository,
            MedicalRecordRepository medicalRecordRepository,
            PrescriptionRepository prescriptionRepository,
            ExamRepository examRepository,
            VaccinationRepository vaccinationRepository,
            AppointmentRepository appointmentRepository,
            ClyvoAiClient clyvoAiClient) {
        this.tutorRepository = tutorRepository;
        this.animalRepository = animalRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.examRepository = examRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.appointmentRepository = appointmentRepository;
        this.clyvoAiClient = clyvoAiClient;
    }

    @Transactional(readOnly = true)
    public AiAnswerResponseDTO askTutor(String tutorCpf, Long animalId, String question) {
        String normalizedQuestion = validateQuestion(question);
        Tutor tutor = tutorRepository.findByCpf(tutorCpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new CustomException("Animal não encontrado", HttpStatus.NOT_FOUND));

        if (!animal.getTutor().getId().equals(tutor.getId())) {
            throw new CustomException("Animal não pertence ao Tutor informado", HttpStatus.FORBIDDEN);
        }

        List<MedicalRecord> records = medicalRecordRepository.findByAnimalIdOrderByCreatedAtDesc(
                animalId,
                PageRequest.of(0, MEDICAL_RECORD_LIMIT)
        );
        List<Long> recordIds = records.stream().map(MedicalRecord::getId).toList();
        List<Prescription> prescriptions = recordIds.isEmpty()
                ? List.of()
                : prescriptionRepository.findByMedicalRecordIdIn(recordIds);
        List<Exam> exams = examRepository.findByMedicalRecordAnimalIdOrderByRequestDateDesc(
                animalId,
                PageRequest.of(0, EXAM_LIMIT)
        );
        List<Vaccination> vaccinations = vaccinationRepository.findByAnimalIdOrderByApplicationDateDesc(
                animalId,
                PageRequest.of(0, VACCINATION_LIMIT)
        );
        List<Appointment> appointments = findRelevantAppointments(animalId);

        ClyvoAiRequestDTO aiRequest = new ClyvoAiRequestDTO(
                normalizedQuestion,
                "TUTOR",
                toAnimalContext(animal),
                records.stream().map(this::toMedicalRecordContext).toList(),
                prescriptions.stream()
                        .sorted(Comparator.comparing(Prescription::getCreatedAt).reversed())
                        .map(this::toPrescriptionContext)
                        .toList(),
                exams.stream().map(this::toExamContext).toList(),
                vaccinations.stream().map(this::toVaccinationContext).toList(),
                appointments.stream().map(this::toAppointmentContext).toList()
        );

        ClyvoAiResponseDTO response = clyvoAiClient.chat(aiRequest);
        return new AiAnswerResponseDTO(response.answer(), response.disclaimer(), response.model());
    }

    private String validateQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new CustomException("A pergunta é obrigatória", HttpStatus.BAD_REQUEST);
        }
        String normalized = question.trim();
        if (normalized.length() > 2000) {
            throw new CustomException("A pergunta deve ter no máximo 2000 caracteres", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private List<Appointment> findRelevantAppointments(Long animalId) {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> appointments = new ArrayList<>(
                appointmentRepository.findTop3ByAnimalIdAndScheduledAtGreaterThanEqualAndStatusInOrderByScheduledAtAsc(
                        animalId,
                        now,
                        List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)
                )
        );
        appointments.addAll(appointmentRepository.findTop5ByAnimalIdAndScheduledAtLessThanAndStatusOrderByScheduledAtDesc(
                animalId,
                now,
                AppointmentStatus.COMPLETED
        ));
        return appointments.stream().limit(APPOINTMENT_LIMIT).toList();
    }

    private AiAnimalContextDTO toAnimalContext(Animal animal) {
        return new AiAnimalContextDTO(
                animal.getId(),
                animal.getName(),
                animal.getSpecies(),
                animal.getRace(),
                animal.getAge(),
                animal.getWeight()
        );
    }

    private AiMedicalRecordContextDTO toMedicalRecordContext(MedicalRecord record) {
        return new AiMedicalRecordContextDTO(
                record.getId(),
                toOffsetDateTime(record.getCreatedAt()),
                record.getDiagnosis(),
                record.getDescription(),
                record.getWeight(),
                record.getTemperature(),
                record.getObservations(),
                record.getVeterinarian().getName()
        );
    }

    private AiPrescriptionContextDTO toPrescriptionContext(Prescription prescription) {
        return new AiPrescriptionContextDTO(
                prescription.getId(),
                toOffsetDateTime(prescription.getCreatedAt()),
                prescription.getMedicalRecord().getVeterinarian().getName(),
                prescription.getItems().stream().map(this::toPrescriptionItemContext).toList()
        );
    }

    private AiPrescriptionItemContextDTO toPrescriptionItemContext(PrescriptionItem item) {
        return new AiPrescriptionItemContextDTO(
                item.getMedicationName(),
                item.getDosage(),
                item.getFrequency(),
                item.getDuration(),
                item.getRoute(),
                item.getInstructions()
        );
    }

    private AiExamContextDTO toExamContext(Exam exam) {
        return new AiExamContextDTO(
                exam.getId(),
                exam.getExamName(),
                exam.getExamType(),
                exam.getStatus().name(),
                toOffsetDateTime(exam.getRequestDate()),
                toOffsetDateTime(exam.getResultDate()),
                exam.getResult(),
                exam.getObservations()
        );
    }

    private AiVaccinationContextDTO toVaccinationContext(Vaccination vaccination) {
        return new AiVaccinationContextDTO(
                vaccination.getId(),
                vaccination.getVaccineName(),
                vaccination.getApplicationDate(),
                vaccination.getNextDoseDate(),
                vaccination.getManufacturer()
        );
    }

    private AiAppointmentContextDTO toAppointmentContext(Appointment appointment) {
        return new AiAppointmentContextDTO(
                appointment.getId(),
                toOffsetDateTime(appointment.getScheduledAt()),
                appointment.getReason(),
                appointment.getStatus().name(),
                appointment.getVeterinarian().getName()
        );
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
