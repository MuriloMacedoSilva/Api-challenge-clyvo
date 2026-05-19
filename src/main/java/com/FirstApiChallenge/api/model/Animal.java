package com.FirstApiChallenge.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Animal {

    @Column(name = "animal_name", length = 150)
    private String name;

    @Column(name = "animal_weight")
    private Float weight;

    @Column(name = "animal_height")
    private Float height;

    private Integer age;

    private String race;

    @Column(columnDefinition = "TEXT")
    private String history;


    public Animal() {
    }

    public Animal(String name, Float weight, Float height, Integer age, String race, String history) {
        this.name = name;
        this.weight = weight;
        this.height = height;
        this.age = age;
        this.race = race;
        this.history = history;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {
        this.history = history;
    }
}