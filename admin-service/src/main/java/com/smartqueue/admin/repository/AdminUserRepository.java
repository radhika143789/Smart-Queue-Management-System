package com.smartqueue.admin.repository;

import com.smartqueue.admin.entity.AdminUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUserEntity, Long> {
    Optional<AdminUserEntity> findByUserId(Long userId);
    Optional<AdminUserEntity> findByEmail(String email);
    List<AdminUserEntity> findByIsActiveTrue();
}
