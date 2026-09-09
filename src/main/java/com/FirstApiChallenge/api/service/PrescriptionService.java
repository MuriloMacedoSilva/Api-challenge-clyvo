package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.PrescriptionItemRequestDTO;
import com.FirstApiChallenge.api.dto.PrescriptionRequestDTO;
import com.FirstApiChallenge.api.dto.PrescriptionResponseDTO;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.MedicalRecord;
import com.FirstApiChallenge.api.model.Prescription;
import com.FirstApiChallenge.api.model.PrescriptionItem;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.repository.MedicalRecordRepository;
import com.FirstApiChallenge.api.repository.PrescriptionRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final VeterinarianTutorLinkRepository linkRepository;
    private final NotificationService notificationService;

    public PrescriptionService(
            PrescriptionRepository prescriptionRepository,
            MedicalRecordRepository medicalRecordRepository,
            TutorRepository tutorRepository,
            VeterinarianRepository veterinarianRepository,
            VeterinarianTutorLinkRepository linkRepository,
            NotificationService notificationService) {
        this.prescriptionRepository = prescriptionRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.tutorRepository = tutorRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.linkRepository = linkRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public PrescriptionResponseDTO create(
            Long medicalRecordId,
            String veterinarianCpf,
            PrescriptionRequestDTO request) {
        MedicalRecord record = findMedicalRecord(medicalRecordId);
        validateResponsibleVeterinarian(record, veterinarianCpf);
        validateAcceptedLink(record.getVeterinarian(), record.getAnimal().getTutor());

        if (record.getAppointment().getStatus() != AppointmentStatus.COMPLETED) {
            throw new CustomException(
                    "A prescrição exige um atendimento concluído",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (prescriptionRepository.existsByMedicalRecordId(medicalRecordId)) {
            throw new CustomException("Já existe prescrição para este prontuário", HttpStatus.CONFLICT);
        }

        validateRequest(request);

        Prescription prescription = new Prescription();
        prescription.setMedicalRecord(record);
        prescription.setInstructions(normalizeOptionalText(request.instructions()));
        replaceItems(prescription, request);

        Prescription savedPrescription = prescriptionRepository.saveAndFlush(prescription);

        notificationService.createTutorNotification(
                record.getAppointment().getTutor(),
                "Uma nova prescrição foi adicionada ao atendimento de "
                        + record.getAnimal().getName() + ".",
                NotificationType.PRESCRIPTION_CREATED
        );

        return PrescriptionResponseDTO.fromEntity(savedPrescription);
    }

    @Transactional
    public PrescriptionResponseDTO update(
            Long prescriptionId,
            String veterinarianCpf,
            PrescriptionRequestDTO request) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new CustomException("Prescrição não encontrada", HttpStatus.NOT_FOUND));
        MedicalRecord record = prescription.getMedicalRecord();

        validateResponsibleVeterinarian(record, veterinarianCpf);
        validateAcceptedLink(record.getVeterinarian(), record.getAnimal().getTutor());
        validateRequest(request);

        prescription.setInstructions(normalizeOptionalText(request.instructions()));
        prescription.setUpdatedAt(LocalDateTime.now());
        replaceItems(prescription, request);

        return PrescriptionResponseDTO.fromEntity(
                prescriptionRepository.saveAndFlush(prescription)
        );
    }

    @Transactional(readOnly = true)
    public Optional<PrescriptionResponseDTO> getForTutor(String tutorCpf, Long medicalRecordId) {
        Tutor tutor = tutorRepository.findByCpf(tutorCpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));
        MedicalRecord record = findMedicalRecord(medicalRecordId);

        if (!record.getAnimal().getTutor().getId().equals(tutor.getId())) {
            throw new CustomException(
                    "Este prontuário não pertence ao tutor informado",
                    HttpStatus.FORBIDDEN
            );
        }

        return findResponseByMedicalRecord(medicalRecordId);
    }

    @Transactional(readOnly = true)
    public Optional<PrescriptionResponseDTO> getForVeterinarian(
            String veterinarianCpf,
            Long medicalRecordId) {
        Veterinarian veterinarian = veterinarianRepository.findByCpf(veterinarianCpf)
                .orElseThrow(() -> new CustomException("Veterinário não encontrado", HttpStatus.NOT_FOUND));
        MedicalRecord record = findMedicalRecord(medicalRecordId);

        validateAcceptedLink(veterinarian, record.getAnimal().getTutor());

        return findResponseByMedicalRecord(medicalRecordId);
    }

    private Optional<PrescriptionResponseDTO> findResponseByMedicalRecord(Long medicalRecordId) {
        return prescriptionRepository.findByMedicalRecordId(medicalRecordId)
                .map(PrescriptionResponseDTO::fromEntity);
    }

    private MedicalRecord findMedicalRecord(Long medicalRecordId) {
        return medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new CustomException("Prontuário não encontrado", HttpStatus.NOT_FOUND));
    }

    private void validateResponsibleVeterinarian(MedicalRecord record, String veterinarianCpf) {
        if (!record.getVeterinarian().getCpf().equals(veterinarianCpf)) {
            throw new CustomException(
                    "Esta prescrição não pertence ao veterinário informado",
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

    private void validateRequest(PrescriptionRequestDTO request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new CustomException(
                    "A prescrição deve possuir pelo menos um medicamento",
                    HttpStatus.BAD_REQUEST
            );
        }

        for (PrescriptionItemRequestDTO item : request.items()) {
            if (item == null) {
                throw new CustomException("Medicamento inválido", HttpStatus.BAD_REQUEST);
            }
            requireText(item.medicationName(), "Nome do medicamento é obrigatório");
            requireText(item.dosage(), "Dosagem é obrigatória");
            requireText(item.frequency(), "Frequência é obrigatória");
            requireText(item.duration(), "Duração é obrigatória");
            requireText(item.route(), "Via de administração é obrigatória");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new CustomException(message, HttpStatus.BAD_REQUEST);
        }
    }

    private void replaceItems(Prescription prescription, PrescriptionRequestDTO request) {
        prescription.getItems().clear();

        for (PrescriptionItemRequestDTO itemRequest : request.items()) {
            PrescriptionItem item = new PrescriptionItem();
            item.setPrescription(prescription);
            item.setMedicationName(itemRequest.medicationName().trim());
            item.setDosage(itemRequest.dosage().trim());
            item.setFrequency(itemRequest.frequency().trim());
            item.setDuration(itemRequest.duration().trim());
            item.setRoute(itemRequest.route().trim());
            item.setInstructions(normalizeOptionalText(itemRequest.instructions()));
            prescription.getItems().add(item);
        }
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
