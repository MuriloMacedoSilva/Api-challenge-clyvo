package com.FirstApiChallenge.api.model;

import com.FirstApiChallenge.api.enums.DevicePlatform;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_device_push_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_push_token", columnNames = "token"),
        check = @CheckConstraint(
                name = "ck_device_push_token_owner",
                constraint = "(tutor_id is not null and veterinarian_id is null) or "
                        + "(tutor_id is null and veterinarian_id is not null)"
        )
)
@Getter
@NoArgsConstructor
public class DevicePushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DevicePlatform platform;

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;

    @ManyToOne
    @JoinColumn(name = "veterinarian_id")
    private Veterinarian veterinarian;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public DevicePushToken(String token, DevicePlatform platform, Tutor tutor) {
        assignToTutor(tutor, platform);
        this.token = token;
        this.active = true;
    }

    public DevicePushToken(String token, DevicePlatform platform, Veterinarian veterinarian) {
        assignToVeterinarian(veterinarian, platform);
        this.token = token;
        this.active = true;
    }

    public void assignToTutor(Tutor tutor, DevicePlatform platform) {
        this.tutor = tutor;
        this.veterinarian = null;
        reactivate(platform);
    }

    public void assignToVeterinarian(Veterinarian veterinarian, DevicePlatform platform) {
        this.tutor = null;
        this.veterinarian = veterinarian;
        reactivate(platform);
    }

    public void reactivate(DevicePlatform platform) {
        this.platform = platform;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
