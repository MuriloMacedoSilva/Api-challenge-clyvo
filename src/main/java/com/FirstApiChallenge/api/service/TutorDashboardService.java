package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.TutorDashboardNextAppointmentDTO;
import com.FirstApiChallenge.api.dto.TutorDashboardRecentActivityDTO;
import com.FirstApiChallenge.api.dto.TutorDashboardResponseDTO;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.AppointmentRepository;
import com.FirstApiChallenge.api.repository.ExamRepository;
import com.FirstApiChallenge.api.repository.MedicalRecordRepository;
import com.FirstApiChallenge.api.repository.PrescriptionRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VaccinationRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TutorDashboardService {

    private static final int RECENT_ACTIVITY_LIMIT = 5;
    private static final List<AppointmentStatus> UPCOMING_STATUSES = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.CONFIRMED
    );

    private final TutorRepository tutorRepository;
    private final AnimalRepository animalRepository;
    private final AppointmentRepository appointmentRepository;
    private final VeterinarianTutorLinkRepository linkRepository;
    private final ExamRepository examRepository;
    private final VaccinationRepository vaccinationRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;

    public TutorDashboardService(
            TutorRepository tutorRepository,
            AnimalRepository animalRepository,
            AppointmentRepository appointmentRepository,
            VeterinarianTutorLinkRepository linkRepository,
            ExamRepository examRepository,
            VaccinationRepository vaccinationRepository,
            MedicalRecordRepository medicalRecordRepository,
            PrescriptionRepository prescriptionRepository) {
        this.tutorRepository = tutorRepository;
        this.animalRepository = animalRepository;
        this.appointmentRepository = appointmentRepository;
        this.linkRepository = linkRepository;
        this.examRepository = examRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    @Transactional(readOnly = true)
    public TutorDashboardResponseDTO getDashboard(String tutorCpf) {
        if (tutorRepository.findByCpf(tutorCpf).isEmpty()) {
            throw new CustomException("Tutor não encontrado.", HttpStatus.NOT_FOUND);
        }

        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDate today = generatedAt.toLocalDate();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        TutorDashboardNextAppointmentDTO nextAppointment = appointmentRepository
                .findFirstByTutorCpfAndScheduledAtGreaterThanEqualAndStatusInOrderByScheduledAtAsc(
                        tutorCpf,
                        generatedAt,
                        UPCOMING_STATUSES
                )
                .map(appointment -> new TutorDashboardNextAppointmentDTO(
                        appointment.getId(),
                        appointment.getAnimal().getId(),
                        appointment.getAnimal().getName(),
                        appointment.getVeterinarian().getName(),
                        appointment.getVeterinarian().getCpf(),
                        appointment.getScheduledAt(),
                        appointment.getReason(),
                        appointment.getStatus()
                ))
                .orElse(null);

        return new TutorDashboardResponseDTO(
                animalRepository.countByTutorCpf(tutorCpf),
                appointmentRepository.countByTutorCpfAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusNot(
                        tutorCpf,
                        todayStart,
                        tomorrowStart,
                        AppointmentStatus.CANCELLED
                ),
                appointmentRepository.countByTutorCpfAndStatus(tutorCpf, AppointmentStatus.PENDING),
                linkRepository.countByTutorCpfAndStatus(tutorCpf, LinkStatus.ACCEPTED),
                examRepository.countByMedicalRecordAnimalTutorCpfAndStatus(tutorCpf, ExamStatus.REQUESTED),
                vaccinationRepository.countByAnimalTutorCpfAndNextDoseDateLessThanEqual(tutorCpf, today),
                nextAppointment,
                buildRecentActivities(tutorCpf),
                generatedAt
        );
    }

    private List<TutorDashboardRecentActivityDTO> buildRecentActivities(String tutorCpf) {
        var limit = PageRequest.of(0, RECENT_ACTIVITY_LIMIT);
        List<TutorDashboardRecentActivityDTO> activities = new ArrayList<>();

        appointmentRepository.findRecentCompletedActivities(tutorCpf, AppointmentStatus.COMPLETED, limit)
                .forEach(item -> activities.add(activity(
                        "APPOINTMENT_COMPLETED",
                        "Consulta concluída",
                        "Consulta de " + item.getAnimalName() + " foi concluída",
                        item.getOccurredAt(),
                        item.getAnimalId(),
                        item.getAnimalName(),
                        item.getReferenceId()
                )));
        medicalRecordRepository.findRecentActivitiesByTutorCpf(tutorCpf, limit)
                .forEach(item -> activities.add(activity(
                        "MEDICAL_RECORD_CREATED",
                        "Prontuário criado",
                        "Novo prontuário para " + item.getAnimalName(),
                        item.getOccurredAt(),
                        item.getAnimalId(),
                        item.getAnimalName(),
                        item.getReferenceId()
                )));
        prescriptionRepository.findRecentActivitiesByTutorCpf(tutorCpf, limit)
                .forEach(item -> activities.add(activity(
                        "PRESCRIPTION_CREATED",
                        "Prescrição criada",
                        "Nova prescrição para " + item.getAnimalName(),
                        item.getOccurredAt(),
                        item.getAnimalId(),
                        item.getAnimalName(),
                        item.getReferenceId()
                )));
        examRepository.findRecentCompletedActivities(tutorCpf, ExamStatus.COMPLETED, limit)
                .forEach(item -> activities.add(activity(
                        "EXAM_RESULT_AVAILABLE",
                        "Resultado de exame disponível",
                        item.getDetail() + " de " + item.getAnimalName(),
                        item.getOccurredAt(),
                        item.getAnimalId(),
                        item.getAnimalName(),
                        item.getReferenceId()
                )));
        vaccinationRepository.findRecentActivitiesByTutorCpf(tutorCpf, limit)
                .forEach(item -> activities.add(activity(
                        "VACCINATION_REGISTERED",
                        "Vacina registrada",
                        item.getAnimalName() + " recebeu " + item.getDetail(),
                        item.getOccurredAt(),
                        item.getAnimalId(),
                        item.getAnimalName(),
                        item.getReferenceId()
                )));

        return activities.stream()
                .sorted(Comparator.comparing(TutorDashboardRecentActivityDTO::occurredAt).reversed()
                        .thenComparing(TutorDashboardRecentActivityDTO::referenceId, Comparator.reverseOrder()))
                .limit(RECENT_ACTIVITY_LIMIT)
                .toList();
    }

    private TutorDashboardRecentActivityDTO activity(
            String type,
            String title,
            String description,
            LocalDateTime occurredAt,
            Long animalId,
            String animalName,
            Long referenceId) {
        return new TutorDashboardRecentActivityDTO(
                type,
                title,
                description,
                occurredAt,
                animalId,
                animalName,
                referenceId
        );
    }
}
