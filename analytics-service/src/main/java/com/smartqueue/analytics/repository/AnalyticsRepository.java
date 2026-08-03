package com.smartqueue.analytics.repository;

import com.smartqueue.analytics.entity.TokenSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TokenSnapshotEntity and analytics queries.
 */
@Repository
public interface AnalyticsRepository extends JpaRepository<TokenSnapshotEntity, Long> {

    Optional<TokenSnapshotEntity> findByTokenId(Long tokenId);

    // Daily totals
    @Query(value = "SELECT COUNT(*) FROM token_snapshots WHERE service_id = :serviceId AND DATE(booked_at) = CAST(:date AS DATE)", nativeQuery = true)
    long countByServiceAndDate(@Param("serviceId") Long serviceId, @Param("date") String date);

    @Query(value = "SELECT COUNT(*) FROM token_snapshots WHERE service_id = :serviceId AND DATE(booked_at) = CAST(:date AS DATE) AND status = :status", nativeQuery = true)
    long countByServiceDateAndStatus(@Param("serviceId") Long serviceId, @Param("date") String date, @Param("status") String status);

    @Query(value = "SELECT AVG(actual_wait_seconds) FROM token_snapshots WHERE service_id = :serviceId AND DATE(booked_at) = CAST(:date AS DATE) AND actual_wait_seconds IS NOT NULL", nativeQuery = true)
    Double avgWaitByServiceAndDate(@Param("serviceId") Long serviceId, @Param("date") String date);

    @Query(value = "SELECT EXTRACT(HOUR FROM booked_at) AS hour, COUNT(*) AS cnt FROM token_snapshots WHERE service_id = :serviceId AND DATE(booked_at) = CAST(:date AS DATE) GROUP BY EXTRACT(HOUR FROM booked_at) ORDER BY cnt DESC LIMIT 1", nativeQuery = true)
    List<Object[]> findPeakHourForDay(@Param("serviceId") Long serviceId, @Param("date") String date);

    // Hourly breakdown for date
    @Query(value = "SELECT EXTRACT(HOUR FROM booked_at) AS hour, COUNT(*) FILTER (WHERE status = 'COMPLETED') AS tokens_completed, COUNT(*) FILTER (WHERE status = 'NO_SHOW') AS tokens_no_show, COUNT(*) FILTER (WHERE status = 'CANCELLED') AS tokens_cancelled, AVG(actual_wait_seconds) FILTER (WHERE actual_wait_seconds IS NOT NULL) AS avg_wait_seconds FROM token_snapshots WHERE service_id = :serviceId AND DATE(booked_at) = CAST(:date AS DATE) GROUP BY EXTRACT(HOUR FROM booked_at) ORDER BY hour", nativeQuery = true)
    List<Object[]> hourlyBreakdown(@Param("serviceId") Long serviceId, @Param("date") String date);

    // Peak hours (30-day rolling)
    @Query(value = "SELECT service_id, EXTRACT(DOW FROM booked_at) AS day_of_week, EXTRACT(HOUR FROM booked_at) AS hour_of_day, COUNT(*) AS avg_tokens, AVG(actual_wait_seconds) AS avg_wait_seconds FROM token_snapshots WHERE service_id = :serviceId AND booked_at > NOW() - INTERVAL '30 days' GROUP BY service_id, EXTRACT(DOW FROM booked_at), EXTRACT(HOUR FROM booked_at)", nativeQuery = true)
    List<Object[]> peakHours(@Param("serviceId") Long serviceId);

    // Weekly count
    @Query(value = "SELECT COUNT(*) FROM token_snapshots WHERE service_id = :serviceId AND booked_at > NOW() - INTERVAL '7 days'", nativeQuery = true)
    long weeklyTotal(@Param("serviceId") Long serviceId);
}
