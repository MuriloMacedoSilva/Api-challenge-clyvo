package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.DashboardMonthlyAppointmentMetricDTO;
import com.FirstApiChallenge.api.dto.DashboardSpeciesMetricDTO;
import com.FirstApiChallenge.api.dto.DashboardUpcomingAppointmentDTO;
import com.FirstApiChallenge.api.dto.VeterinarianDashboardResponseDTO;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.AppointmentRepository;
import com.FirstApiChallenge.api.repository.ExamRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VeterinarianDashboardService {

    private static final int MONTHS_IN_SERIES = 6;
    private static final List<AppointmentStatus> UPCOMING_STATUSES = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.CONFIRMED
    );
    private static final String[] MONTH_LABELS = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private final VeterinarianRepository veterinarianRepository;
    private final VeterinarianTutorLinkRepository linkRepository;
    private final AnimalRepository animalRepository;
    private final AppointmentRepository appointmentRepository;
    private final ExamRepository examRepository;

    public VeterinarianDashboardService(
            VeterinarianRepository veterinarianRepository,
            VeterinarianTutorLinkRepository linkRepository,
            AnimalRepository animalRepository,
            AppointmentRepository appointmentRepository,
            ExamRepository examRepository) {
        this.veterinarianRepository = veterinarianRepository;
        this.linkRepository = linkRepository;
        this.animalRepository = animalRepository;
        this.appointmentRepository = appointmentRepository;
        this.examRepository = examRepository;
    }

    @Transactional(readOnly = true)
    public VeterinarianDashboardResponseDTO getDashboard(String veterinarianCpf) {
        if (!veterinarianRepository.findByCpf(veterinarianCpf).isPresent()) {
            throw new CustomException("Veterinário não encontrado.", HttpStatus.NOT_FOUND);
        }

        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime todayStart = generatedAt.toLocalDate().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        List<YearMonth> months = lastSixMonths(YearMonth.from(generatedAt));
        LocalDateTime seriesStart = months.getFirst().atDay(1).atStartOfDay();
        LocalDateTime seriesEnd = YearMonth.from(generatedAt).plusMonths(1).atDay(1).atStartOfDay();

        long linkedTutors = linkRepository.countByVeterinarianCpfAndStatus(
                veterinarianCpf,
                LinkStatus.ACCEPTED
        );
        long activePatients = animalRepository.countByAcceptedVeterinarianLink(
                veterinarianCpf,
                LinkStatus.ACCEPTED
        );
        long appointmentsToday = appointmentRepository
                .countByVeterinarianCpfAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusNot(
                        veterinarianCpf,
                        todayStart,
                        tomorrowStart,
                        AppointmentStatus.CANCELLED
                );

        Map<AppointmentStatus, Long> appointmentsByStatus = new EnumMap<>(AppointmentStatus.class);
        appointmentRepository.countByStatusForVeterinarian(veterinarianCpf)
                .forEach(metric -> appointmentsByStatus.put(metric.getStatus(), metric.getCount()));

        long pendingExams = examRepository.countByMedicalRecordVeterinarianCpfAndStatus(
                veterinarianCpf,
                ExamStatus.REQUESTED
        );

        List<DashboardMonthlyAppointmentMetricDTO> monthlyMetrics = buildMonthlyMetrics(
                months,
                appointmentRepository.findScheduledAtByVeterinarianAndStatusInPeriod(
                        veterinarianCpf,
                        AppointmentStatus.COMPLETED,
                        seriesStart,
                        seriesEnd
                )
        );

        List<DashboardSpeciesMetricDTO> speciesMetrics = buildSpeciesMetrics(
                animalRepository.countBySpeciesAndAcceptedVeterinarianLink(
                        veterinarianCpf,
                        LinkStatus.ACCEPTED
                )
        );

        List<DashboardUpcomingAppointmentDTO> upcomingAppointments = appointmentRepository
                .findTop5ByVeterinarianCpfAndScheduledAtGreaterThanEqualAndStatusInOrderByScheduledAtAsc(
                        veterinarianCpf,
                        generatedAt,
                        UPCOMING_STATUSES
                )
                .stream()
                .map(appointment -> new DashboardUpcomingAppointmentDTO(
                        appointment.getId(),
                        appointment.getAnimal().getId(),
                        appointment.getAnimal().getName(),
                        appointment.getTutor().getName(),
                        appointment.getScheduledAt(),
                        appointment.getReason(),
                        appointment.getStatus()
                ))
                .toList();

        return new VeterinarianDashboardResponseDTO(
                linkedTutors,
                activePatients,
                appointmentsToday,
                appointmentsByStatus.getOrDefault(AppointmentStatus.PENDING, 0L),
                appointmentsByStatus.getOrDefault(AppointmentStatus.CONFIRMED, 0L),
                appointmentsByStatus.getOrDefault(AppointmentStatus.COMPLETED, 0L),
                pendingExams,
                monthlyMetrics,
                speciesMetrics,
                upcomingAppointments,
                generatedAt
        );
    }

    static List<YearMonth> lastSixMonths(YearMonth currentMonth) {
        List<YearMonth> months = new ArrayList<>(MONTHS_IN_SERIES);
        for (int offset = MONTHS_IN_SERIES - 1; offset >= 0; offset--) {
            months.add(currentMonth.minusMonths(offset));
        }
        return months;
    }

    private List<DashboardMonthlyAppointmentMetricDTO> buildMonthlyMetrics(
            List<YearMonth> months,
            List<LocalDateTime> completedAppointments) {
        Map<YearMonth, Long> counts = new LinkedHashMap<>();
        months.forEach(month -> counts.put(month, 0L));
        completedAppointments.forEach(scheduledAt -> counts.computeIfPresent(
                YearMonth.from(scheduledAt),
                (month, count) -> count + 1
        ));

        return counts.entrySet().stream()
                .map(entry -> new DashboardMonthlyAppointmentMetricDTO(
                        entry.getKey().getYear(),
                        entry.getKey().getMonthValue(),
                        MONTH_LABELS[entry.getKey().getMonthValue() - 1],
                        entry.getValue()
                ))
                .toList();
    }

    private List<DashboardSpeciesMetricDTO> buildSpeciesMetrics(
            List<AnimalRepository.SpeciesCountProjection> projections) {
        Map<String, Long> counts = new LinkedHashMap<>();
        projections.forEach(metric -> {
            String species = metric.getSpecies();
            String label = species == null || species.isBlank() ? "Não informado" : species.trim();
            counts.merge(label, metric.getCount(), Long::sum);
        });

        return counts.entrySet().stream()
                .map(entry -> new DashboardSpeciesMetricDTO(entry.getKey(), entry.getValue()))
                .sorted((first, second) -> {
                    int countComparison = Long.compare(second.count(), first.count());
                    return countComparison != 0
                            ? countComparison
                            : first.species().compareToIgnoreCase(second.species());
                })
                .toList();
    }
}
