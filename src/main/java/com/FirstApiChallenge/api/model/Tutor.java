package com.FirstApiChallenge.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Tutor")
@Getter
@Setter
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
    @Size(min = 8, max = 11, message = "O campo deve ter entre 8 e 11 caracteres")
    private String password;

    @Column(nullable = false)
    private String role;

    @ManyToMany(mappedBy = "tutors")
    private Set<Veterinarian> veterinarians = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "tutor_animals", // O Hibernate criará uma tabela auxiliar só para guardar os animais associados ao ID do tutor
            joinColumns = @JoinColumn(name = "tutor_id")
    )
    private Set<Animal> animals = new HashSet<>();

    public Tutor() {
    };

    public Tutor(String name, String email, String cpf, String phoneNumber, String password, Set<Veterinarian> veterinarians, Set<Animal> animals) {
        this.name = name;
        this.email = email;
        this.cpf = cpf;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.veterinarians = veterinarians;
        this.animals = animals;
    };

    public Set<Animal> getAnimals() {
        return animals;
    }

    public void setAnimals(Set<Animal> animals) {
        this.animals = animals;
    }

    public Set<Veterinarian> getVeterinarians() {
        return veterinarians;
    }

    public void setVeterinarians(Set<Veterinarian> veterinarians) {
        this.veterinarians = veterinarians;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}