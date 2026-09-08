package com.FirstApiChallenge.api.model;

import com.FirstApiChallenge.api.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    private boolean isRead = false;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    @JsonIgnoreProperties({"notifications", "links"})
    private Tutor tutor;

    @ManyToOne
    @JoinColumn(name = "veterinarian_id")
    @JsonIgnoreProperties({"notifications", "links"})
    private Veterinarian veterinarian;

    @ManyToOne
    @JoinColumn(name = "link_id", nullable = true)
    @JsonIgnoreProperties({"tutor", "veterinarian"})
    private VeterinarianTutorLink link;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Construtor incluindo o link para solicitação de vínculo
    public Notification(String message, NotificationType type, Tutor tutor, VeterinarianTutorLink link) {
        this.message = message;
        this.type = type;
        this.tutor = tutor;
        this.link = link;
        this.createdAt = LocalDateTime.now();
    }

    // Construtor utilitário para criar notificação do Tutor
    public Notification(String message, NotificationType type, Tutor tutor) {
        this.message = message;
        this.type = type;
        this.tutor = tutor;
        this.createdAt = LocalDateTime.now();
    }

    // Construtor utilitário para criar notificação do Veterinário
    public Notification(String message, NotificationType type, Veterinarian veterinarian) {
        this.message = message;
        this.type = type;
        this.veterinarian = veterinarian;
        this.createdAt = LocalDateTime.now();
    }
}