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

        String history // Opcional, sem anotação de obrigatoriedade
) {
    // Método utilitário para converter de DTO para Entidade
    public Animal toEntity() {
        return new Animal(
                this.name,
                this.weight,
                this.height,
                this.age,
                this.race,
                this.species,
                this.history
        );
    }
}