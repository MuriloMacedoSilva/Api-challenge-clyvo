package com.FirstApiChallenge.api.model;

import com.FirstApiChallenge.api.enums.LinkStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_veterinarian_tutor_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_veterinarian_tutor_link",
                columnNames = {"veterinarian_id", "tutor_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VeterinarianTutorLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "veterinarian_id", nullable = false)
    private Veterinarian veterinarian;

    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LinkStatus status;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    // Adicione este construtor de 2 parâmetros:
    public VeterinarianTutorLink(Veterinarian veterinarian, Tutor tutor) {
        this.veterinarian = veterinarian;
        this.tutor = tutor;
        this.status = LinkStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
}
