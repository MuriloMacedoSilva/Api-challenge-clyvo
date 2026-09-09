package com.FirstApiChallenge.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

//@Entity
//@Table(name = "Tutor")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class Tutor {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, length = 200)
//    private String name;
//
//    @Column(nullable = false, length = 400)
//    private String email;
//
//    @Column(nullable = false, length = 11)
//    private String cpf;
//
//    @Column(nullable = false, length = 11)
//    private String phoneNumber;
//
//    @Column(nullable = false)
//    @Size(min = 8, max = 11, message = "O campo deve ter entre 8 e 11 caracteres")
//    private String password;
//
//    @Column(nullable = false)
//    private String role;
//
//    @ManyToMany(mappedBy = "tutors")
//    private Set<Veterinarian> veterinarians = new HashSet<>();
//
//    @ElementCollection
//    @CollectionTable(
//            name = "tutor_animals", // O Hibernate criará uma tabela auxiliar só para guardar os animais associados ao ID do tutor
//            joinColumns = @JoinColumn(name = "tutor_id")
//    )
//    private Set<Animal> animals = new HashSet<>();
//
//}



@Entity
@Table(name = "Tutor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tutor {

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
    @Size(min = 8, max = 11)
    private String password;

    @Column(nullable = false)
    private String role;

//    @ManyToMany(mappedBy = "tutors")
//    private Set<Veterinarian> veterinarians = new HashSet<>();

    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Animal> animals = new HashSet<>();
}