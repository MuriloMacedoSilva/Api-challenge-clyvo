package com.FirstApiChallenge.api.dto;

import com.FirstApiChallenge.api.model.Animal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AnimalDTO(

        @NotBlank(message = "O nome do pet é obrigatório")
        String name,

        @NotNull(message = "O peso do pet é obrigatório")
        @Positive(message = "O peso deve ser maior que zero")
        Float weight,

        @NotNull(message = "A altura do pet é obrigatória")
        @Positive(message = "A altura deve ser maior que zero")
        Float height,

        @NotNull(message = "A idade do pet é obrigatória")
        @Positive(message = "A idade deve ser um valor positivo")
        Integer age,

        @NotBlank(message = "A raça do pet é obrigatória")
        String race,

        @NotBlank(message = "A espécie do pet é obrigatória")
        String species,

        String history

) {

    public Animal toEntity() {

        Animal animal = new Animal();

        animal.setName(this.name());
        animal.setWeight(this.weight());
        animal.setHeight(this.height());
        animal.setAge(this.age());
        animal.setRace(this.race());
        animal.setSpecies(this.species());
        animal.setHistory(this.history());

        return animal;
    }
}
