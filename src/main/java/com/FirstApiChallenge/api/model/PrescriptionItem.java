package com.FirstApiChallenge.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_prescription_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(nullable = false, length = 200)
    private String medicationName;

    @Column(nullable = false, length = 200)
    private String dosage;

    @Column(nullable = false, length = 200)
    private String frequency;

    @Column(nullable = false, length = 200)
    private String duration;

    @Column(nullable = false, length = 100)
    private String route;

    @Column(length = 1000)
    private String instructions;
}
