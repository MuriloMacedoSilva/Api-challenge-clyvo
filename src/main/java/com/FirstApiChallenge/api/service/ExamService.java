package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.ExamRequestDTO;
import com.FirstApiChallenge.api.dto.ExamResponseDTO;
import com.FirstApiChallenge.api.dto.ExamResultRequestDTO;
import com.FirstApiChallenge.api.enums.AppointmentStatus;
import com.FirstApiChallenge.api.enums.ExamStatus;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Exam;
import com.FirstApiChallenge.api.model.MedicalRecord;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.ExamRepository;
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
public class ExamService {

    private final ExamRepository examRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final AnimalRepository animalRepository;
    private final VeterinarianTutorLinkRepository linkRepository;
    private final NotificationService notificationService;

    public ExamService(
            ExamRepository examRepository,
            MedicalRecordRepository medicalRecordRepository,
            TutorRepository tutorRepository,
            VeterinarianRepository veterinarianRepository,
            AnimalRepository animalRepository,
            VeterinarianTutorLinkRepository linkRepository,
            NotificationService notificationService) {
        this.examRepository = examRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.tutorRepository = tutorRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.animalRepository = animalRepository;
        this.linkRepository = linkRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ExamResponseDTO create(
            Long medicalRecordId,
            String veterinarianCpf,
            ExamRequestDTO request) {
        MedicalRecord record = findMedicalRecord(medicalRecordId);
        validateMutationAccess(record, veterinarianCpf);

        if (record.getAppointment().getStatus() != AppointmentStatus.COMPLETED) {
            throw new CustomException(
                    "A solicitação de exame exige um atendimento concluído",
                    HttpStatus.BAD_REQUEST
            );
        }

        validateExamRequest(request);

        Exam exam = new Exam();
        exam.setMedicalRecord(record);
        exam.setExamName(request.examName().trim());
        exam.setExamType(normalizeOptionalText(request.examType()));
        exam.setObservations(normalizeOptionalText(request.observations()));
        exam.setStatus(ExamStatus.REQUESTED);
        exam.setRequestDate(LocalDateTime.now());

        Exam savedExam = examRepository.save(exam);

        notificationService.createTutorNotification(
                record.getAppointment().getTutor(),
                "Foi solicitado o exame \"" + abbreviate(exam.getExamName()) + "\" para "
                        + abbreviate(record.getAnimal().getName()) + ".",
                NotificationType.EXAM_REQUESTED
        );

        return ExamResponseDTO.fromEntity(savedExam);
    }

    @Transactional
    public ExamResponseDTO update(
            Long examId,
            String veterinarianCpf,
            ExamRequestDTO request) {
        Exam exam = findExamForUpdate(examId);
        validateMutationAccess(exam.getMedicalRecord(), veterinarianCpf);
        validateRequestedStatus(exam, "Apenas exames solicitados podem ser editados");
        validateExamRequest(request);

        exam.setExamName(request.examName().trim());
        exam.setExamType(normalizeOptionalText(request.examType()));
        exam.setObservations(normalizeOptionalText(request.observations()));
        exam.setUpdatedAt(LocalDateTime.now());

        return ExamResponseDTO.fromEntity(examRepository.saveAndFlush(exam));
    }

    @Transactional
    public ExamResponseDTO registerResult(
            Long examId,
            String veterinarianCpf,
            ExamResultRequestDTO request) {
        Exam exam = findExamForUpdate(examId);
        validateMutationAccess(exam.getMedicalRecord(), veterinarianCpf);
        validateRequestedStatus(exam, "Apenas exames solicitados podem receber resultado");

        if (request.result() == null || request.result().isBlank()) {
            throw new CustomException("Resultado do exame é obrigatório", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        exam.setResult(request.result().trim());
        exam.setResultDate(now);
        exam.setStatus(ExamStatus.COMPLETED);
        exam.setUpdatedAt(now);
        exam.setObservations(normalizeOptionalText(request.observations()));

        Exam savedExam = examRepository.saveAndFlush(exam);
        MedicalRecord record = exam.getMedicalRecord();

        notificationService.createTutorNotification(
                record.getAppointment().getTutor(),
                "O resultado do exame \"" + abbreviate(exam.getExamName()) + "\" de "
                        + abbreviate(record.getAnimal().getName()) + " já está disponível.",
                NotificationType.EXAM_RESULT_AVAILABLE
        );

        return ExamResponseDTO.fromEntity(savedExam);
    }

    @Transactional
    public ExamResponseDTO cancel(Long examId, String veterinarianCpf) {
        Exam exam = findExamForUpdate(examId);
        validateMutationAccess(exam.getMedicalRecord(), veterinarianCpf);
        validateRequestedStatus(exam, "Apenas exames solicitados podem ser cancelados");

        exam.setStatus(ExamStatus.CANCELLED);
        exam.setUpdatedAt(LocalDateTime.now());

        Exam savedExam = examRepository.saveAndFlush(exam);
        MedicalRecord record = exam.getMedicalRecord();

        notificationService.createTutorNotification(
                record.getAppointment().getTutor(),
                "O exame \"" + abbreviate(exam.getExamName()) + "\" de "
                        + abbreviate(record.getAnimal().getName()) + " foi cancelado.",
                NotificationType.EXAM_CANCELLED
        );

        return ExamResponseDTO.fromEntity(savedExam);
    }

    @Transactional(readOnly = true)
    public List<ExamResponseDTO> getByMedicalRecordForTutor(
            String tutorCpf,
            Long medicalRecordId) {
        Tutor tutor = findTutor(tutorCpf);
        MedicalRecord record = findMedicalRecord(medicalRecordId);
        validateTutorOwnership(tutor, record.getAnimal());

        return mapExams(examRepository.findByMedicalRecordIdOrderByRequestDateDesc(medicalRecordId));
    }

    @Transactional(readOnly = true)
    public List<ExamResponseDTO> getByMedicalRecordForVeterinarian(
            String veterinarianCpf,
            Long medicalRecordId) {
        Veterinarian veterinarian = findVeterinarian(veterinarianCpf);
        MedicalRecord record = findMedicalRecord(medicalRecordId);
        validateAcceptedLink(veterinarian, record.getAnimal().getTutor());

        return mapExams(examRepository.findByMedicalRecordIdOrderByRequestDateDesc(medicalRecordId));
    }

    @Transactional(readOnly = true)
    public List<ExamResponseDTO> getByAnimalForTutor(String tutorCpf, Long animalId) {
        Tutor tutor = findTutor(tutorCpf);
        Animal animal = findAnimal(animalId);
        validateTutorOwnership(tutor, animal);

        return mapExams(examRepository.findByMedicalRecordAnimalIdOrderByRequestDateDesc(animalId));
    }

    @Transactional(readOnly = true)
    public List<ExamResponseDTO> getByAnimalForVeterinarian(
            String veterinarianCpf,
            Long animalId) {
        Veterinarian veterinarian = findVeterinarian(veterinarianCpf);
        Animal animal = findAnimal(animalId);
        validateAcceptedLink(veterinarian, animal.getTutor());

        return mapExams(examRepository.findByMedicalRecordAnimalIdOrderByRequestDateDesc(animalId));
    }

    private List<ExamResponseDTO> mapExams(List<Exam> exams) {
        return exams.stream().map(ExamResponseDTO::fromEntity).toList();
    }

    private Exam findExamForUpdate(Long examId) {
        return examRepository.findByIdForUpdate(examId)
                .orElseThrow(() -> new CustomException("Exame não encontrado", HttpStatus.NOT_FOUND));
    }

    private MedicalRecord findMedicalRecord(Long medicalRecordId) {
        return medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new CustomException("Prontuário não encontrado", HttpStatus.NOT_FOUND));
    }

    private Tutor findTutor(String tutorCpf) {
        return tutorRepository.findByCpf(tutorCpf)
                .orElseThrow(() -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND));
    }

    private Veterinarian findVeterinarian(String veterinarianCpf) {
        return veterinarianRepository.findByCpf(veterinarianCpf)
                .orElseThrow(() -> new CustomException("Veterinário não encontrado", HttpStatus.NOT_FOUND));
    }

    private Animal findAnimal(Long animalId) {
        return animalRepository.findById(animalId)
                .orElseThrow(() -> new CustomException("Animal não encontrado", HttpStatus.NOT_FOUND));
    }

    private void validateMutationAccess(MedicalRecord record, String veterinarianCpf) {
        if (!record.getVeterinarian().getCpf().equals(veterinarianCpf)) {
            throw new CustomException(
                    "Este exame não pertence ao veterinário informado",
                    HttpStatus.FORBIDDEN
            );
        }
        validateAcceptedLink(record.getVeterinarian(), record.getAnimal().getTutor());
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

    private void validateTutorOwnership(Tutor tutor, Animal animal) {
        if (!animal.getTutor().getId().equals(tutor.getId())) {
            throw new CustomException("Este animal não pertence ao tutor informado", HttpStatus.FORBIDDEN);
        }
    }

    private void validateRequestedStatus(Exam exam, String message) {
        if (exam.getStatus() != ExamStatus.REQUESTED) {
            throw new CustomException(message, HttpStatus.BAD_REQUEST);
        }
    }

    private void validateExamRequest(ExamRequestDTO request) {
        if (request.examName() == null || request.examName().isBlank()) {
            throw new CustomException("Nome do exame é obrigatório", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String abbreviate(String value) {
        return value.length() <= 80 ? value : value.substring(0, 77) + "...";
    }
}
