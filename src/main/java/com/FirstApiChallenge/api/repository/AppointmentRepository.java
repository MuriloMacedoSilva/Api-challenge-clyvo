package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
