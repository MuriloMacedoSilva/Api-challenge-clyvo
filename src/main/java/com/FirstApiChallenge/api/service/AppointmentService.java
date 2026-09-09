package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.AppointmentRequestDTO;
import com.FirstApiChallenge.api.dto.AppointmentResponseDTO;
import com.FirstApiChallenge.api.dto.LinkedVeterinarianResponseDTO;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Appointment;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.AppointmentRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AppointmentService {

    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final TutorRepository tutorRepository;
    private final AnimalRepository animalRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final VeterinarianTutorLinkRepository linkRepository;
    private final NotificationService notificationService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            TutorRepository tutorRepository,
            AnimalRepository animalRepository,
            VeterinarianRepository veterinarianRepository,
            VeterinarianTutorLinkRepository linkRepository,
            NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.tutorRepository = tutorRepository;
        this.animalRepository = animalRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.linkRepository = linkRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public AppointmentResponseDTO create(String tutorCpf, AppointmentRequestDTO request) {
        Tutor tutor = tutorRepository.findByCpf(tutorCpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));

        Animal animal = animalRepository.findById(request.animalId())
                .orElseThrow(() -> new CustomException("Animal não encontrado", HttpStatus.NOT_FOUND));

        if (!animal.getTutor().getId().equals(tutor.getId())) {
            throw new CustomException("Este animal não pertence ao tutor informado", HttpStatus.FORBIDDEN);
        }

        Veterinarian veterinarian = veterinarianRepository.findByCpf(request.veterinarianCpf())
                .orElseThrow(() -> new CustomException("Veterinário não encontrado", HttpStatus.NOT_FOUND));

        boolean hasAcceptedLink = linkRepository.existsByVeterinarianCpfAndTutorCpfAndStatus(
                veterinarian.getCpf(),
                tutor.getCpf(),
                LinkStatus.ACCEPTED
        );

        if (!hasAcceptedLink) {
            throw new CustomException(
                    "Tutor não possui vínculo aceito com este veterinário",
                    HttpStatus.FORBIDDEN
            );
        }

        if (request.scheduledAt() == null || !request.scheduledAt().isAfter(LocalDateTime.now())) {
            throw new CustomException("A data da consulta deve estar no futuro", HttpStatus.BAD_REQUEST);
        }

        boolean scheduleConflict = appointmentRepository
                .existsByVeterinarianIdAndScheduledAtAndStatusIn(
                        veterinarian.getId(),
                        request.scheduledAt(),
                        ACTIVE_STATUSES
                );

        if (scheduleConflict) {
            throw new CustomException(
                    "O veterinário já possui uma consulta ativa neste horário",
                    HttpStatus.CONFLICT
            );
        }

        Appointment appointment = new Appointment();
        appointment.setAnimal(animal);
        appointment.setTutor(tutor);
        appointment.setVeterinarian(veterinarian);
        appointment.setScheduledAt(request.scheduledAt());
        appointment.setReason(request.reason().trim());
        appointment.setStatus(AppointmentStatus.PENDING);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        notificationService.createVeterinarianNotification(
                veterinarian,
                "Nova solicitação de consulta para o animal " + animal.getName()
                        + " em " + formatDate(appointment.getScheduledAt()) + ".",
                NotificationType.APPOINTMENT_REQUESTED
        );

        return AppointmentResponseDTO.fromEntity(savedAppointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getTutorAppointments(String tutorCpf) {
        tutorRepository.findByCpf(tutorCpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));

        return appointmentRepository.findByTutorCpfOrderByScheduledAtAsc(tutorCpf)
                .stream()
                .map(AppointmentResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getVeterinarianAppointments(String veterinarianCpf) {
        veterinarianRepository.findByCpf(veterinarianCpf)
                .orElseThrow(() -> new CustomException("Veterinário não encontrado", HttpStatus.NOT_FOUND));

        return appointmentRepository.findByVeterinarianCpfOrderByScheduledAtAsc(veterinarianCpf)
                .stream()
                .map(AppointmentResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LinkedVeterinarianResponseDTO> getLinkedVeterinarians(String tutorCpf) {
        tutorRepository.findByCpf(tutorCpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));

        return linkRepository.findByTutorCpfAndStatus(tutorCpf, LinkStatus.ACCEPTED)
                .stream()
                .map(link -> LinkedVeterinarianResponseDTO.fromEntity(link.getVeterinarian()))
                .toList();
    }

    @Transactional
    public AppointmentResponseDTO confirm(Long appointmentId, String veterinarianCpf) {
        Appointment appointment = findAppointment(appointmentId);
        validateVeterinarian(appointment, veterinarianCpf);

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new CustomException("Apenas consultas pendentes podem ser confirmadas", HttpStatus.BAD_REQUEST);
        }

        changeStatus(appointment, AppointmentStatus.CONFIRMED);

        notificationService.createTutorNotification(
                appointment.getTutor(),
                "Sua consulta para " + appointment.getAnimal().getName()
                        + " foi confirmada pelo Dr(a). " + appointment.getVeterinarian().getName() + ".",
                NotificationType.APPOINTMENT_CONFIRMED
        );

        return AppointmentResponseDTO.fromEntity(appointment);
    }

    @Transactional
    public AppointmentResponseDTO cancelByTutor(Long appointmentId, String tutorCpf) {
        Appointment appointment = findAppointment(appointmentId);
        validateTutor(appointment, tutorCpf);
        validateCancellation(appointment);
        changeStatus(appointment, AppointmentStatus.CANCELLED);

        notificationService.createVeterinarianNotification(
                appointment.getVeterinarian(),
                "O tutor " + appointment.getTutor().getName() + " cancelou a consulta de "
                        + appointment.getAnimal().getName() + ".",
                NotificationType.APPOINTMENT_CANCELLED
        );

        return AppointmentResponseDTO.fromEntity(appointment);
    }

    @Transactional
    public AppointmentResponseDTO cancelByVeterinarian(Long appointmentId, String veterinarianCpf) {
        Appointment appointment = findAppointment(appointmentId);
        validateVeterinarian(appointment, veterinarianCpf);
        validateCancellation(appointment);
        changeStatus(appointment, AppointmentStatus.CANCELLED);

        notificationService.createTutorNotification(
                appointment.getTutor(),
                "O Dr(a). " + appointment.getVeterinarian().getName() + " cancelou a consulta de "
                        + appointment.getAnimal().getName() + ".",
                NotificationType.APPOINTMENT_CANCELLED
        );

        return AppointmentResponseDTO.fromEntity(appointment);
    }

    private Appointment findAppointment(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new CustomException("Agendamento não encontrado", HttpStatus.NOT_FOUND));
    }

    private void validateTutor(Appointment appointment, String tutorCpf) {
        if (!appointment.getTutor().getCpf().equals(tutorCpf)) {
            throw new CustomException("Este agendamento não pertence ao tutor informado", HttpStatus.FORBIDDEN);
        }
    }

    private void validateVeterinarian(Appointment appointment, String veterinarianCpf) {
        if (!appointment.getVeterinarian().getCpf().equals(veterinarianCpf)) {
            throw new CustomException("Este agendamento não pertence ao veterinário informado", HttpStatus.FORBIDDEN);
        }
    }

    private void validateCancellation(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new CustomException("Esta consulta não pode mais ser cancelada", HttpStatus.BAD_REQUEST);
        }
    }

    private void changeStatus(Appointment appointment, AppointmentStatus status) {
        appointment.setStatus(status);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    private String formatDate(LocalDateTime date) {
        return date.format(DATE_FORMAT);
    }
}
