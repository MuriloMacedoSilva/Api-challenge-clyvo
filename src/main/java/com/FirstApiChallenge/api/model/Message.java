package com.FirstApiChallenge.api.model;

import com.FirstApiChallenge.api.enums.MessageSenderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageSenderType senderType;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    @PrePersist
    public void prePersist() {
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }
}
