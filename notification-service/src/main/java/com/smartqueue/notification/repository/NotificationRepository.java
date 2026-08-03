package com.smartqueue.notification.repository;

import com.smartqueue.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByReferenceId(String referenceId);

    List<NotificationEntity> findByStatusAndRetryCountLessThan(String status, int maxRetry);

    long countByStatusAndCreatedAtAfter(String status, Instant after);
}
