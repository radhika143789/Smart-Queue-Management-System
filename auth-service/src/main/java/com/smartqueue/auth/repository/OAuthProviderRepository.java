package com.smartqueue.auth.repository;

import com.smartqueue.auth.entity.OAuthProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthProviderRepository extends JpaRepository<OAuthProviderEntity, Long> {
    Optional<OAuthProviderEntity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
