package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByTutorCpfOrderByCreatedAtDesc(String cpf);

    List<Notification> findByVeterinarianCpfOrderByCreatedAtDesc(String cpf);

    void deleteByTutorCpf(String cpf);

    void deleteByVeterinarianCpf(String cpf);
}
