package com.FirstApiChallenge.api;

import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import com.FirstApiChallenge.api.repository.VeterinarianTutorLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:clyvo-tutor-animal-read-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
class TutorAnimalReadIntegrationTests {

    private static final String TUTOR_CPF = "11122233344";
    private static final String VETERINARIAN_CPF = "99988877766";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private VeterinarianTutorLinkRepository linkRepository;

    @BeforeEach
    void setUp() {
        linkRepository.deleteAll();
        animalRepository.deleteAll();
        veterinarianRepository.deleteAll();
        tutorRepository.deleteAll();
    }

    @Test
    void readsTutorAnimalsAfterRepositorySessionsAreClosed() throws Exception {
        Tutor tutor = createTutor();
        createAnimal(tutor, "Luna", "Historico da Luna");
        createAnimal(tutor, "Thor", "Historico do Thor");

        mockMvc.perform(get("/tutor/{cpf}/read-animals", TUTOR_CPF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Luna", "Thor")))
                .andExpect(jsonPath("$[*].history", containsInAnyOrder(
                        "Historico da Luna",
                        "Historico do Thor"
                )));
    }

    @Test
    void returnsEmptyAnimalCollectionForExistingTutor() throws Exception {
        createTutor();

        mockMvc.perform(get("/tutor/{cpf}/read-animals", TUTOR_CPF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void readsLinkedTutorsAndTheirAnimalsWithOpenInViewDisabled() throws Exception {
        Tutor tutor = createTutor();
        createAnimal(tutor, "Luna", "Historico da Luna");
        Veterinarian veterinarian = createVeterinarian();
        VeterinarianTutorLink link = new VeterinarianTutorLink(veterinarian, tutor);
        link.setStatus(LinkStatus.ACCEPTED);
        linkRepository.save(link);

        mockMvc.perform(get("/v1/links/veterinarian/{cpf}/tutors", VETERINARIAN_CPF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cpf").value(TUTOR_CPF))
                .andExpect(jsonPath("$[0].animals", hasSize(1)))
                .andExpect(jsonPath("$[0].animals[0].name").value("Luna"));
    }

    private Tutor createTutor() {
        Tutor tutor = new Tutor();
        tutor.setName("Tutor Teste");
        tutor.setEmail("tutor@teste.com");
        tutor.setCpf(TUTOR_CPF);
        tutor.setPhoneNumber("11999999999");
        tutor.setPassword("12345678");
        tutor.setRole("tutor");
        return tutorRepository.save(tutor);
    }

    private Veterinarian createVeterinarian() {
        Veterinarian veterinarian = new Veterinarian();
        veterinarian.setName("Veterinaria Teste");
        veterinarian.setEmail("veterinaria@teste.com");
        veterinarian.setCpf(VETERINARIAN_CPF);
        veterinarian.setPhoneNumber("11888888888");
        veterinarian.setPassword("12345678");
        veterinarian.setRole("veterinarian");
        veterinarian.setCrmvNumber("12345");
        veterinarian.setCrmvState("SP");
        veterinarian.setCnpj("12345678000199");
        return veterinarianRepository.save(veterinarian);
    }

    private void createAnimal(Tutor tutor, String name, String history) {
        Animal animal = new Animal();
        animal.setTutor(tutor);
        animal.setName(name);
        animal.setWeight(8.5f);
        animal.setHeight(0.4f);
        animal.setAge(3);
        animal.setRace("SRD");
        animal.setSpecies("Cachorro");
        animal.setHistory(history);
        animalRepository.save(animal);
    }
}
