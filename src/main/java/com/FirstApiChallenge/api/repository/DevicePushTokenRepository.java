package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.model.DevicePushToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface DevicePushTokenRepository extends JpaRepository<DevicePushToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from DevicePushToken token where token.token = :token")
    Optional<DevicePushToken> findByTokenForUpdate(@Param("token") String token);

    Optional<DevicePushToken> findByToken(String token);

    List<DevicePushToken> findByTutorIdAndActiveTrue(Long tutorId);

    List<DevicePushToken> findByVeterinarianIdAndActiveTrue(Long veterinarianId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update DevicePushToken token
            set token.active = false, token.updatedAt = CURRENT_TIMESTAMP
            where token.token = :token
              and token.active = true
              and token.tutor.id = :tutorId
              and token.veterinarian is null
            """)
    int deactivateActiveTutorToken(@Param("token") String token, @Param("tutorId") Long tutorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update DevicePushToken token
            set token.active = false, token.updatedAt = CURRENT_TIMESTAMP
            where token.token = :token
              and token.active = true
              and token.veterinarian.id = :veterinarianId
              and token.tutor is null
            """)
    int deactivateActiveVeterinarianToken(
            @Param("token") String token,
            @Param("veterinarianId") Long veterinarianId
    );
}
