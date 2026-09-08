package com.FirstApiChallenge.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Animal {

    @Column(name = "animal_name", length = 150, nullable = false)
    private String name;

    @Column(name = "animal_weight", nullable = false)
    private Float weight;

    @Column(name = "animal_height", nullable = false)
    private Float height;

    @Column(name = "animal_age", nullable = false)
    private Integer age;

    @Column(nullable = false)
    private String race;

    @Column(nullable = false)
    private String species;

    @Column(columnDefinition = "TEXT")
    private String history;

}