package com.FirstApiChallenge.api;

import com.FirstApiChallenge.api.model.Animal;
import com.FirstApiChallenge.api.model.Tutor;
import com.FirstApiChallenge.api.repository.AnimalRepository;
import com.FirstApiChallenge.api.repository.TutorRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApiApplicationTests {

	@Autowired
	private TutorRepository tutorRepository;

	@Autowired
	private AnimalRepository animalRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void storesAnimalHistoryLargerThanLegacyVarcharLimit() {
		var tutor = new Tutor();
		tutor.setName("Tutor history mapping");
		tutor.setEmail("history-mapping@example.com");
		tutor.setCpf("55555555555");
		tutor.setPhoneNumber("11999999999");
		tutor.setPassword("12345678");
		tutor.setRole("TUTOR");
		tutorRepository.save(tutor);

		var history = "H".repeat(40_000);
		var animal = new Animal();
		animal.setName("History mapping animal");
		animal.setWeight(10.0F);
		animal.setHeight(30.0F);
		animal.setAge(5);
		animal.setRace("Mixed");
		animal.setSpecies("Dog");
		animal.setHistory(history);
		animal.setTutor(tutor);
		animalRepository.saveAndFlush(animal);

		var animalId = animal.getId();
		entityManager.clear();

		assertThat(animalRepository.findById(animalId))
				.get()
				.extracting(Animal::getHistory)
				.isEqualTo(history);
	}

}
