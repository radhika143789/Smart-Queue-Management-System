package com.smartqueue.auth.repository;

import com.smartqueue.auth.entity.RefreshTokenEntity;
import com.smartqueue.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    List<RefreshTokenEntity> findAllByUser(UserEntity user);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.user = :user")
    void deleteAllByUser(@Param("user") UserEntity user);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.user = :user")
    void revokeAllByUser(@Param("user") UserEntity user);

    void deleteByExpiresAtBefore(Instant now);
}
