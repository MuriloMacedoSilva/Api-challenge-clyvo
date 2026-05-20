package com.FirstApiChallenge.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Veterinarian")
@Getter
@Setter
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
    @Column
    private String crmvState; // Ex: "SP"

    @Column(nullable = false, length = 14)
    private String cnpj;

    @ManyToMany
    @JoinTable(name = "veterinario_tutor")
    private Set<Tutor> tutors = new HashSet<>();

    public Veterinarian() {
    }

    public Veterinarian(Long id, String name, String email, String cpf, String phoneNumber, String password, String role, String crmvNumber, String crmvState, String cnpj, Set<Tutor> tutors) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.cpf = cpf;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.role = role;
        this.crmvNumber = crmvNumber;
        this.crmvState = crmvState;
        this.cnpj = cnpj;
        this.tutors = tutors;
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

    public String getCrmvNumber() {
        return crmvNumber;
    }

    public void setCrmvNumber(String crmvNumber) {
        this.crmvNumber = crmvNumber;
    }

    public String getCrmvState() {
        return crmvState;
    }

    public void setCrmvState(String crmvState) {
        this.crmvState = crmvState;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public Set<Tutor> getTutors() {
        return tutors;
    }

    public void setTutors(Set<Tutor> tutors) {
        this.tutors = tutors;
    }
}