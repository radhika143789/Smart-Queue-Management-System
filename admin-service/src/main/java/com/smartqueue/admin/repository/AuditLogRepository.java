package com.smartqueue.admin.repository;

import com.smartqueue.admin.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    Page<AuditLogEntity> findByActorUserIdOrderByOccurredAtDesc(Long userId, Pageable pageable);
    Page<AuditLogEntity> findByActionOrderByOccurredAtDesc(String action, Pageable pageable);
    Page<AuditLogEntity> findByOccurredAtAfter(Instant after, Pageable pageable);
}
