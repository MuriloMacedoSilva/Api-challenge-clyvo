package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.enums.MessageSenderType;
import com.FirstApiChallenge.api.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAscIdAsc(Long conversationId);

    Optional<Message> findTopByConversationIdOrderBySentAtDescIdDesc(Long conversationId);

    long countByConversationIdAndSenderTypeAndReadFalse(
            Long conversationId,
            MessageSenderType senderType
    );

    @Query("""
            select m
            from Message m
            where m.id in (
                select max(latest.id)
                from Message latest
                where latest.conversation.id in :conversationIds
                group by latest.conversation.id
            )
            """)
    List<Message> findLatestByConversationIds(@Param("conversationIds") List<Long> conversationIds);

    @Query("""
            select m.conversation.id as conversationId, count(m) as unreadCount
            from Message m
            where m.conversation.id in :conversationIds
              and m.senderType = :senderType
              and m.read = false
            group by m.conversation.id
            """)
    List<UnreadCountProjection> countUnreadByConversationIds(
            @Param("conversationIds") List<Long> conversationIds,
            @Param("senderType") MessageSenderType senderType
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Message m
            set m.read = true, m.readAt = :readAt
            where m.conversation.id = :conversationId
              and m.senderType = :senderType
              and m.read = false
              and m.id <= :upToMessageId
            """)
    int markUnreadAsRead(
            @Param("conversationId") Long conversationId,
            @Param("senderType") MessageSenderType senderType,
            @Param("upToMessageId") Long upToMessageId,
            @Param("readAt") LocalDateTime readAt
    );

    interface UnreadCountProjection {
        Long getConversationId();

        Long getUnreadCount();
    }
}
