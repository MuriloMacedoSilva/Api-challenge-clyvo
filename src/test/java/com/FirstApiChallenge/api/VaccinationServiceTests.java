package com.FirstApiChallenge.api;

import com.FirstApiChallenge.api.dto.VaccinationRequestDTO;
import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.enums.NotificationType;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.NotificationRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VaccinationRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import com.FirstApiChallenge.api.service.VaccinationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class VaccinationServiceTests {

    @Autowired
    private VaccinationService vaccinationService;

    @Autowired
    private VaccinationRepository vaccinationRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private VeterinarianTutorLinkRepository linkRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Tutor tutor;
    private Veterinarian author;
    private Animal animal;

    @BeforeEach
    void setUp() {
        tutor = tutorRepository.save(createTutor("12345678901", "Tutor A"));
        author = veterinarianRepository.save(createVeterinarian("10987654321", "Vet A", "1234"));
        animal = animalRepository.save(createAnimal(tutor, "Luna"));
        addAcceptedLink(author, tutor);
    }

    @Test
    void createsVaccinationAndNotifiesTutor() {
        var response = vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("V10", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1), "  ")
        );

        assertNotNull(response.id());
        assertEquals(animal.getId(), response.animalId());
        assertEquals(author.getCpf(), response.veterinarianCpf());
        assertEquals("1234", response.veterinarianCrmvNumber());
        assertNull(response.batchNumber());
        assertNotNull(response.createdAt());
        assertTrue(notificationRepository.findByTutorCpfOrderByCreatedAtDesc(tutor.getCpf())
                .stream()
                .anyMatch(notification -> notification.getType() == NotificationType.VACCINATION_REGISTERED));
    }

    @Test
    void rejectsVeterinarianWithoutAcceptedLink() {
        Veterinarian unlinked = veterinarianRepository.save(
                createVeterinarian("22233344455", "Vet B", "5678")
        );

        CustomException exception = assertThrows(CustomException.class, () -> vaccinationService.create(
                animal.getId(),
                unlinked.getCpf(),
                request("V10", LocalDate.now(), null, null)
        ));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void rejectsFutureApplicationDate() {
        CustomException exception = assertThrows(CustomException.class, () -> vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("V10", LocalDate.now().plusDays(1), null, null)
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void rejectsNextDoseBeforeApplicationDate() {
        LocalDate applicationDate = LocalDate.now().minusDays(2);

        CustomException exception = assertThrows(CustomException.class, () -> vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("V10", applicationDate, applicationDate.minusDays(1), null)
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void rejectsNextDoseEqualToApplicationDate() {
        LocalDate applicationDate = LocalDate.now().minusDays(1);

        CustomException exception = assertThrows(CustomException.class, () -> vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("V10", applicationDate, applicationDate, null)
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void authorUpdatesVaccination() {
        var created = vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("V10", LocalDate.now().minusMonths(1), null, null)
        );

        var updated = vaccinationService.update(
                created.id(),
                author.getCpf(),
                new VaccinationRequestDTO(
                        "Antirrábica",
                        LocalDate.now().minusDays(1),
                        LocalDate.now().plusYears(1),
                        "LOTE-2",
                        "Fabricante X",
                        "Sem intercorrências"
                )
        );

        assertEquals("Antirrábica", updated.vaccineName());
        assertEquals("LOTE-2", updated.batchNumber());
        assertNotNull(updated.updatedAt());
    }

    @Test
    void otherLinkedVeterinarianCannotUpdateVaccination() {
        var created = vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("V10", LocalDate.now(), null, null)
        );
        Veterinarian other = veterinarianRepository.save(
                createVeterinarian("22233344455", "Vet B", "5678")
        );
        addAcceptedLink(other, tutor);

        CustomException exception = assertThrows(CustomException.class, () -> vaccinationService.update(
                created.id(),
                other.getCpf(),
                request("V8", LocalDate.now(), null, null)
        ));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("V10", vaccinationRepository.findById(created.id()).orElseThrow().getVaccineName());
    }

    @Test
    void tutorReadsHistoryOrderedByApplicationDateDescending() {
        vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("V8", LocalDate.now().minusYears(2), null, null)
        );
        vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("V10", LocalDate.now(), null, null)
        );
        vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("Antirrábica", LocalDate.now().minusYears(1), null, null)
        );

        var history = vaccinationService.getByAnimalForTutor(tutor.getCpf(), animal.getId());

        assertEquals(3, history.size());
        assertEquals("V10", history.get(0).vaccineName());
        assertEquals("Antirrábica", history.get(1).vaccineName());
        assertEquals("V8", history.get(2).vaccineName());
    }

    @Test
    void wrongTutorCannotReadHistory() {
        Tutor otherTutor = tutorRepository.save(createTutor("22233344455", "Tutor B"));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> vaccinationService.getByAnimalForTutor(otherTutor.getCpf(), animal.getId())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void linkedVeterinarianReadsVaccinationCreatedByAnotherVeterinarian() {
        vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("V10", LocalDate.now(), null, null)
        );
        Veterinarian other = veterinarianRepository.save(
                createVeterinarian("22233344455", "Vet B", "5678")
        );
        addAcceptedLink(other, tutor);

        var history = vaccinationService.getByAnimalForVeterinarian(other.getCpf(), animal.getId());

        assertEquals(1, history.size());
        assertEquals(author.getCpf(), history.get(0).veterinarianCpf());
    }

    @Test
    void validAnimalWithoutVaccinationsReturnsEmptyList() {
        assertTrue(vaccinationService.getByAnimalForTutor(tutor.getCpf(), animal.getId()).isEmpty());
        assertTrue(vaccinationService.getByAnimalForVeterinarian(author.getCpf(), animal.getId()).isEmpty());
    }

    @Test
    void repeatedVaccineNameIsAllowedForDifferentApplications() {
        vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("V10", LocalDate.now().minusYears(1), null, null)
        );
        vaccinationService.create(
                animal.getId(),
                author.getCpf(),
                request("V10", LocalDate.now(), LocalDate.now().plusYears(1), null)
        );

        var history = vaccinationService.getByAnimalForTutor(tutor.getCpf(), animal.getId());

        assertEquals(2, history.size());
        assertFalse(history.get(0).applicationDate().equals(history.get(1).applicationDate()));
    }

    private VaccinationRequestDTO request(
            String vaccineName,
            LocalDate applicationDate,
            LocalDate nextDoseDate,
            String batchNumber) {
        return new VaccinationRequestDTO(
                vaccineName,
                applicationDate,
                nextDoseDate,
                batchNumber,
                "Fabricante",
                "Aplicação sem intercorrências"
        );
    }

    private void addAcceptedLink(Veterinarian veterinarian, Tutor linkedTutor) {
        VeterinarianTutorLink link = new VeterinarianTutorLink(veterinarian, linkedTutor);
        link.setStatus(LinkStatus.ACCEPTED);
        linkRepository.save(link);
    }

    private Animal createAnimal(Tutor owner, String name) {
        Animal value = new Animal();
        value.setName(name);
        value.setWeight(8.5f);
        value.setHeight(0.4f);
        value.setAge(3);
        value.setRace("SRD");
        value.setSpecies("Cachorro");
        value.setHistory("");
        value.setTutor(owner);
        return value;
    }

    private Tutor createTutor(String cpf, String name) {
        Tutor value = new Tutor();
        value.setName(name);
        value.setEmail(cpf + "@email.com");
        value.setCpf(cpf);
        value.setPhoneNumber("11999999999");
        value.setPassword("12345678");
        value.setRole("tutor");
        return value;
    }

    private Veterinarian createVeterinarian(String cpf, String name, String crmv) {
        Veterinarian value = new Veterinarian();
        value.setName(name);
        value.setEmail(cpf + "@email.com");
        value.setCpf(cpf);
        value.setPhoneNumber("11999999999");
        value.setPassword("12345678");
        value.setRole("veterinarian");
        value.setCrmvNumber(crmv);
        value.setCrmvState("SP");
        value.setCnpj("12345678000199");
        return value;
    }
}
