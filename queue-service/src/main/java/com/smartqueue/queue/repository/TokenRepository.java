package com.smartqueue.queue.repository;

import com.smartqueue.common.enums.TokenStatus;
import com.smartqueue.queue.entity.TokenEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<TokenEntity, Long> {

    List<TokenEntity> findByServiceIdAndStatusOrderBySequenceNumberAsc(Long serviceId, TokenStatus status);

    @Query("SELECT COUNT(t) FROM TokenEntity t WHERE t.service.id = :serviceId AND t.status = 'WAITING' AND t.sequenceNumber < :seq")
    long countWaitingAhead(@Param("serviceId") Long serviceId, @Param("seq") int seq);

    Optional<TokenEntity> findByUserIdAndServiceIdAndStatusIn(Long userId, Long serviceId, List<TokenStatus> statuses);

    @Query(value = "SELECT COALESCE(MAX(t.sequence_number), 0) FROM tokens t WHERE t.service_id = :serviceId AND DATE(t.booked_at) = CURRENT_DATE", nativeQuery = true)
    int findMaxSequenceForToday(@Param("serviceId") Long serviceId);

    long countByServiceIdAndStatus(Long serviceId, TokenStatus status);
    
    @Query("SELECT t FROM TokenEntity t WHERE t.service.id = :serviceId")
    Page<TokenEntity> findTokensByServiceId(@Param("serviceId") Long serviceId, Pageable pageable);
}
