package com.FirstApiChallenge.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Veterinarian")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Veterinarian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 400)
    private String email;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(nullable = false, length = 11)
    private String phoneNumber;

    @Column(nullable = false)
    @Size(min = 8, max = 11, message = "O campo deve ter entre 8 e 11 caracteres")
    private String password;

    @Column(nullable = false)
    private String role;

    @Size(min = 3, max = 6, message = "O número do CRMV deve ter entre 3 e 6 dígitos")
    @Column(length = 6)
    private String crmvNumber;

    @Size(min = 2, max = 2)
    private String crmvState; // Ex: "SP"

    @Column(nullable = false, length = 14)
    private String cnpj;

//    @ManyToMany
//    @JoinTable(name = "veterinario_tutor")
//    private Set<Tutor> tutors = new HashSet<>();


}