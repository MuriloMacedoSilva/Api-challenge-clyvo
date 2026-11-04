package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;

@Component
@Profile("!oracle")
public class JpaDashboardMetricsReader implements DashboardMetricsReader {

    private final AnimalRepository animalRepository;
    private final AppointmentRepository appointmentRepository;
    private final VeterinarianTutorLinkRepository linkRepository;
    private final ExamRepository examRepository;
    private final VaccinationRepository vaccinationRepository;

    public JpaDashboardMetricsReader(
            AnimalRepository animalRepository,
            AppointmentRepository appointmentRepository,
            VeterinarianTutorLinkRepository linkRepository,
            ExamRepository examRepository,
            VaccinationRepository vaccinationRepository) {
        this.animalRepository = animalRepository;
        this.appointmentRepository = appointmentRepository;
        this.linkRepository = linkRepository;
        this.examRepository = examRepository;
        this.vaccinationRepository = vaccinationRepository;
    }

    @Override
    public TutorMetrics readTutorMetrics(
            Long tutorId,
            String tutorCpf,
            LocalDateTime referenceTime) {
        var today = referenceTime.toLocalDate();
        var todayStart = today.atStartOfDay();
        var tomorrowStart = today.plusDays(1).atStartOfDay();

        return new TutorMetrics(
                animalRepository.countByTutorCpf(tutorCpf),
                appointmentRepository
                        .countByTutorCpfAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusNot(
                                tutorCpf,
                                todayStart,
                                tomorrowStart,
                                AppointmentStatus.CANCELLED
                        ),
                appointmentRepository.countByTutorCpfAndStatus(
                        tutorCpf,
                        AppointmentStatus.PENDING
                ),
                linkRepository.countByTutorCpfAndStatus(tutorCpf, LinkStatus.ACCEPTED),
                examRepository.countByMedicalRecordAnimalTutorCpfAndStatus(
                        tutorCpf,
                        ExamStatus.REQUESTED
                ),
                vaccinationRepository.countByAnimalTutorCpfAndNextDoseDateLessThanEqual(
                        tutorCpf,
                        today
                )
        );
    }

    @Override
    public VeterinarianMetrics readVeterinarianMetrics(
            Long veterinarianId,
            String veterinarianCpf,
            LocalDateTime referenceTime) {
        var todayStart = referenceTime.toLocalDate().atStartOfDay();
        var tomorrowStart = todayStart.plusDays(1);
        var appointmentsByStatus = new EnumMap<AppointmentStatus, Long>(AppointmentStatus.class);
        appointmentRepository.countByStatusForVeterinarian(veterinarianCpf)
                .forEach(metric -> appointmentsByStatus.put(metric.getStatus(), metric.getCount()));

        return new VeterinarianMetrics(
                linkRepository.countByVeterinarianCpfAndStatus(
                        veterinarianCpf,
                        LinkStatus.ACCEPTED
                ),
                animalRepository.countByAcceptedVeterinarianLink(
                        veterinarianCpf,
                        LinkStatus.ACCEPTED
                ),
                appointmentRepository
                        .countByVeterinarianCpfAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusNot(
                                veterinarianCpf,
                                todayStart,
                                tomorrowStart,
                                AppointmentStatus.CANCELLED
                        ),
                appointmentsByStatus.getOrDefault(AppointmentStatus.PENDING, 0L),
                appointmentsByStatus.getOrDefault(AppointmentStatus.CONFIRMED, 0L),
                appointmentsByStatus.getOrDefault(AppointmentStatus.COMPLETED, 0L),
                examRepository.countByMedicalRecordVeterinarianCpfAndStatus(
                        veterinarianCpf,
                        ExamStatus.REQUESTED
                )
        );
    }
}
