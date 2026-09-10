package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.VaccinationRequestDTO;
import com.FirstApiChallenge.api.dto.VaccinationResponseDTO;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Vaccination;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VaccinationRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class VaccinationService {

    private final VaccinationRepository vaccinationRepository;
    private final AnimalRepository animalRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final VeterinarianTutorLinkRepository linkRepository;
    private final NotificationService notificationService;

    public VaccinationService(
            VaccinationRepository vaccinationRepository,
            AnimalRepository animalRepository,
            TutorRepository tutorRepository,
            VeterinarianRepository veterinarianRepository,
            VeterinarianTutorLinkRepository linkRepository,
            NotificationService notificationService) {
        this.vaccinationRepository = vaccinationRepository;
        this.animalRepository = animalRepository;
        this.tutorRepository = tutorRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.linkRepository = linkRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public VaccinationResponseDTO create(
            Long animalId,
            String veterinarianCpf,
            VaccinationRequestDTO request) {
        Veterinarian veterinarian = findVeterinarian(veterinarianCpf);
        Animal animal = findAnimal(animalId);
        validateAcceptedLink(veterinarian, animal.getTutor());
        validateRequest(request);

        Vaccination vaccination = new Vaccination();
        vaccination.setAnimal(animal);
        vaccination.setVeterinarian(veterinarian);
        applyRequest(vaccination, request);

        Vaccination savedVaccination = vaccinationRepository.saveAndFlush(vaccination);

        notificationService.createTutorNotification(
                animal.getTutor(),
                "A vacina \"" + vaccination.getVaccineName() + "\" foi registrada para "
                        + animal.getName() + ".",
                NotificationType.VACCINATION_REGISTERED
        );

        return VaccinationResponseDTO.fromEntity(savedVaccination);
    }

    @Transactional
    public VaccinationResponseDTO update(
            Long vaccinationId,
            String veterinarianCpf,
            VaccinationRequestDTO request) {
        Vaccination vaccination = findVaccination(vaccinationId);
        Veterinarian veterinarian = findVeterinarian(veterinarianCpf);

        if (!vaccination.getVeterinarian().getId().equals(veterinarian.getId())) {
            throw new CustomException(
                    "Esta vacinação não pertence ao veterinário informado",
                    HttpStatus.FORBIDDEN
            );
        }

        validateAcceptedLink(veterinarian, vaccination.getAnimal().getTutor());
        validateRequest(request);
        applyRequest(vaccination, request);

        return VaccinationResponseDTO.fromEntity(vaccinationRepository.saveAndFlush(vaccination));
    }

    @Transactional(readOnly = true)
    public List<VaccinationResponseDTO> getByAnimalForTutor(String tutorCpf, Long animalId) {
        Tutor tutor = findTutor(tutorCpf);
        Animal animal = findAnimal(animalId);

        if (!animal.getTutor().getId().equals(tutor.getId())) {
            throw new CustomException(
                    "Este animal não pertence ao tutor informado",
                    HttpStatus.FORBIDDEN
            );
        }

        return mapVaccinations(vaccinationRepository.findByAnimalIdOrderByApplicationDateDesc(animalId));
    }

    @Transactional(readOnly = true)
    public List<VaccinationResponseDTO> getByAnimalForVeterinarian(
            String veterinarianCpf,
            Long animalId) {
        Veterinarian veterinarian = findVeterinarian(veterinarianCpf);
        Animal animal = findAnimal(animalId);
        validateAcceptedLink(veterinarian, animal.getTutor());

        return mapVaccinations(vaccinationRepository.findByAnimalIdOrderByApplicationDateDesc(animalId));
    }

    private List<VaccinationResponseDTO> mapVaccinations(List<Vaccination> vaccinations) {
        return vaccinations.stream().map(VaccinationResponseDTO::fromEntity).toList();
    }

    private Vaccination findVaccination(Long vaccinationId) {
        return vaccinationRepository.findById(vaccinationId)
                .orElseThrow(() -> new CustomException("Vacinação não encontrada", HttpStatus.NOT_FOUND));
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

    private void validateRequest(VaccinationRequestDTO request) {
        if (request.vaccineName() == null || request.vaccineName().isBlank()) {
            throw new CustomException("Nome da vacina é obrigatório", HttpStatus.BAD_REQUEST);
        }
        if (request.applicationDate() == null) {
            throw new CustomException("Data de aplicação é obrigatória", HttpStatus.BAD_REQUEST);
        }
        if (request.applicationDate().isAfter(LocalDate.now())) {
            throw new CustomException(
                    "Data de aplicação não pode estar no futuro",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (request.nextDoseDate() != null
                && !request.nextDoseDate().isAfter(request.applicationDate())) {
            throw new CustomException(
                    "Próxima dose deve ser posterior à data de aplicação",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void applyRequest(Vaccination vaccination, VaccinationRequestDTO request) {
        vaccination.setVaccineName(request.vaccineName().trim());
        vaccination.setApplicationDate(request.applicationDate());
        vaccination.setNextDoseDate(request.nextDoseDate());
        vaccination.setBatchNumber(normalizeOptionalText(request.batchNumber()));
        vaccination.setManufacturer(normalizeOptionalText(request.manufacturer()));
        vaccination.setObservations(normalizeOptionalText(request.observations()));
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
