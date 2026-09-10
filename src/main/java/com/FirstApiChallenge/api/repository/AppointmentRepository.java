package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.model.Appointment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByTutorCpfOrderByScheduledAtAsc(String tutorCpf);

    List<Appointment> findByVeterinarianCpfOrderByScheduledAtAsc(String veterinarianCpf);

    boolean existsByVeterinarianIdAndScheduledAtAndStatusIn(
            Long veterinarianId,
            LocalDateTime scheduledAt,
            Collection<AppointmentStatus> statuses
    );

    long countByVeterinarianCpfAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusNot(
            String veterinarianCpf,
            LocalDateTime start,
            LocalDateTime end,
            AppointmentStatus excludedStatus
    );

    @Query("""
            select appointment.status as status, count(appointment.id) as count
            from Appointment appointment
            where appointment.veterinarian.cpf = :veterinarianCpf
            group by appointment.status
            """)
    List<StatusCountProjection> countByStatusForVeterinarian(
            @Param("veterinarianCpf") String veterinarianCpf
    );

    @Query("""
            select appointment.scheduledAt
            from Appointment appointment
            where appointment.veterinarian.cpf = :veterinarianCpf
              and appointment.status = :status
              and appointment.scheduledAt >= :start
              and appointment.scheduledAt < :end
            """)
    List<LocalDateTime> findScheduledAtByVeterinarianAndStatusInPeriod(
            @Param("veterinarianCpf") String veterinarianCpf,
            @Param("status") AppointmentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @EntityGraph(attributePaths = {"animal", "tutor"})
    List<Appointment> findTop5ByVeterinarianCpfAndScheduledAtGreaterThanEqualAndStatusInOrderByScheduledAtAsc(
            String veterinarianCpf,
            LocalDateTime scheduledAt,
            Collection<AppointmentStatus> statuses
    );

    interface StatusCountProjection {
        AppointmentStatus getStatus();

        Long getCount();
    }
}
