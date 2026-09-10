package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByTutorIdAndVeterinarianId(Long tutorId, Long veterinarianId);

    @EntityGraph(attributePaths = {"tutor", "veterinarian"})
    List<Conversation> findByTutorCpfOrderByUpdatedAtDesc(String tutorCpf);

    @EntityGraph(attributePaths = {"tutor", "veterinarian"})
    List<Conversation> findByVeterinarianCpfOrderByUpdatedAtDesc(String veterinarianCpf);
}
