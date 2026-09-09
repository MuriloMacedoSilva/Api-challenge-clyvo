package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.MedicalRecordRequestDTO;
import com.FirstApiChallenge.api.dto.MedicalRecordResponseDTO;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Appointment;
import com.FirstApiChallenge.api.model.MedicalRecord;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.AppointmentRepository;
import com.FirstApiChallenge.api.repository.MedicalRecordRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final AnimalRepository animalRepository;
    private final VeterinarianTutorLinkRepository linkRepository;
    private final NotificationService notificationService;

    public MedicalRecordService(
            MedicalRecordRepository medicalRecordRepository,
            AppointmentRepository appointmentRepository,
            TutorRepository tutorRepository,
            VeterinarianRepository veterinarianRepository,
            AnimalRepository animalRepository,
            VeterinarianTutorLinkRepository linkRepository,
            NotificationService notificationService) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.appointmentRepository = appointmentRepository;
        this.tutorRepository = tutorRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.animalRepository = animalRepository;
        this.linkRepository = linkRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public MedicalRecordResponseDTO create(
            Long appointmentId,
            String veterinarianCpf,
            MedicalRecordRequestDTO request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new CustomException("Agendamento não encontrado", HttpStatus.NOT_FOUND));

        validateVeterinarian(appointment.getVeterinarian(), veterinarianCpf);
        validateAcceptedLink(appointment.getVeterinarian(), appointment.getTutor());

        if (medicalRecordRepository.existsByAppointmentId(appointmentId)) {
            throw new CustomException("Já existe prontuário para esta consulta", HttpStatus.CONFLICT);
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new CustomException(
                    "A consulta deve estar confirmada para registrar o atendimento",
                    HttpStatus.BAD_REQUEST
            );
        }

        validateClinicalData(request);

        MedicalRecord record = new MedicalRecord();
        record.setAppointment(appointment);
        record.setAnimal(appointment.getAnimal());
        record.setVeterinarian(appointment.getVeterinarian());
        applyClinicalData(record, request);

        MedicalRecord savedRecord = medicalRecordRepository.save(record);

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        notificationService.createTutorNotification(
                appointment.getTutor(),
                "O atendimento de " + appointment.getAnimal().getName()
                        + " foi concluído e um novo registro foi adicionado ao histórico clínico.",
                NotificationType.MEDICAL_RECORD_CREATED
        );

        return MedicalRecordResponseDTO.fromEntity(savedRecord);
    }

    @Transactional
    public MedicalRecordResponseDTO update(
            Long recordId,
            String veterinarianCpf,
            MedicalRecordRequestDTO request) {
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException("Prontuário não encontrado", HttpStatus.NOT_FOUND));

        validateVeterinarian(record.getVeterinarian(), veterinarianCpf);
        validateAcceptedLink(record.getVeterinarian(), record.getAnimal().getTutor());
        validateClinicalData(request);
        applyClinicalData(record, request);
        record.setUpdatedAt(LocalDateTime.now());

        return MedicalRecordResponseDTO.fromEntity(medicalRecordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDTO> getAnimalHistoryForTutor(String tutorCpf, Long animalId) {
        Tutor tutor = tutorRepository.findByCpf(tutorCpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));
        Animal animal = findAnimal(animalId);

        if (!animal.getTutor().getId().equals(tutor.getId())) {
            throw new CustomException("Este animal não pertence ao tutor informado", HttpStatus.FORBIDDEN);
        }

        return getAnimalHistory(animalId);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDTO> getAnimalHistoryForVeterinarian(
            String veterinarianCpf,
            Long animalId) {
        Veterinarian veterinarian = veterinarianRepository.findByCpf(veterinarianCpf)
                .orElseThrow(() -> new CustomException("Veterinário não encontrado", HttpStatus.NOT_FOUND));
        Animal animal = findAnimal(animalId);

        validateAcceptedLink(veterinarian, animal.getTutor());

        return getAnimalHistory(animalId);
    }

    private List<MedicalRecordResponseDTO> getAnimalHistory(Long animalId) {
        return medicalRecordRepository.findByAnimalIdOrderByCreatedAtDesc(animalId)
                .stream()
                .map(MedicalRecordResponseDTO::fromEntity)
                .toList();
    }

    private Animal findAnimal(Long animalId) {
        return animalRepository.findById(animalId)
                .orElseThrow(() -> new CustomException("Animal não encontrado", HttpStatus.NOT_FOUND));
    }

    private void validateVeterinarian(Veterinarian veterinarian, String veterinarianCpf) {
        if (!veterinarian.getCpf().equals(veterinarianCpf)) {
            throw new CustomException(
                    "Este atendimento não pertence ao veterinário informado",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private void validateAcceptedLink(Veterinarian veterinarian, Tutor tutor) {
        boolean hasAcceptedLink = linkRepository.existsByVeterinarianCpfAndTutorCpfAndStatus(
                veterinarian.getCpf(),
                tutor.getCpf(),
                LinkStatus.ACCEPTED
        );

        if (!hasAcceptedLink) {
            throw new CustomException(
                    "Veterinário não possui vínculo aceito com o tutor deste animal",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private void validateClinicalData(MedicalRecordRequestDTO request) {
        if (request.diagnosis() == null || request.diagnosis().isBlank()) {
            throw new CustomException("Diagnóstico é obrigatório", HttpStatus.BAD_REQUEST);
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new CustomException("Descrição clínica é obrigatória", HttpStatus.BAD_REQUEST);
        }
        if (request.weight() != null && request.weight() <= 0) {
            throw new CustomException("Peso deve ser maior que zero", HttpStatus.BAD_REQUEST);
        }
        if (request.temperature() != null && request.temperature() <= 0) {
            throw new CustomException("Temperatura deve ser maior que zero", HttpStatus.BAD_REQUEST);
        }
    }

    private void applyClinicalData(MedicalRecord record, MedicalRecordRequestDTO request) {
        record.setDiagnosis(request.diagnosis().trim());
        record.setDescription(request.description().trim());
        record.setWeight(request.weight());
        record.setTemperature(request.temperature());
        record.setObservations(normalizeOptionalText(request.observations()));
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
