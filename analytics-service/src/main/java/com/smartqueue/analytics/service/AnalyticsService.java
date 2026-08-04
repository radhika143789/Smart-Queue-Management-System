package com.smartqueue.analytics.service;

import com.smartqueue.analytics.dto.DailyStatsResponse;
import com.smartqueue.analytics.dto.HourlyStatsResponse;
import com.smartqueue.analytics.dto.PeakHoursResponse;
import com.smartqueue.analytics.dto.ServiceSummaryResponse;
import com.smartqueue.analytics.entity.TokenSnapshotEntity;
import com.smartqueue.analytics.repository.AnalyticsRepository;
import com.smartqueue.common.event.QueueEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analytics operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    private static final String[] DAY_NAMES = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    @Transactional
    public void upsertSnapshot(QueueEvent event) {
        Optional<TokenSnapshotEntity> optionalSnapshot = analyticsRepository.findByTokenId(event.getTokenId());
        TokenSnapshotEntity snapshot = optionalSnapshot.orElseGet(() -> TokenSnapshotEntity.builder()
                .tokenId(event.getTokenId())
                .tokenNumber(event.getTokenNumber())
                .serviceId(event.getServiceId())
                .serviceName(event.getServiceName())
                .userId(event.getUserId())
                .bookedAt(event.getOccurredAt())
                .estimatedWaitSeconds(event.getEstimatedWaitSeconds())
                .build());

        snapshot.setEventType(event.getEventType());
        String status = mapEventToStatus(event.getEventType());
        if (status != null) {
            snapshot.setStatus(status);
        }

        switch (event.getEventType()) {
            case "TOKEN_CALLED":
                snapshot.setCalledAt(event.getOccurredAt());
                break;
            case "TOKEN_COMPLETED":
                snapshot.setCompletedAt(event.getOccurredAt());
                if (snapshot.getBookedAt() != null && event.getOccurredAt() != null) {
                    long actualWait = java.time.Duration.between(snapshot.getBookedAt(), event.getOccurredAt()).getSeconds();
                    snapshot.setActualWaitSeconds((int) actualWait);
                }
                break;
            case "TOKEN_CANCELLED":
                // Assuming cancellation might happen without being called
                break;
            default:
                break;
        }

        analyticsRepository.save(snapshot);
        log.debug("Upserted snapshot for token {}: status {}", event.getTokenId(), snapshot.getStatus());
    }

    private String mapEventToStatus(String eventType) {
        return switch (eventType) {
            case "TOKEN_BOOKED" -> "WAITING";
            case "TOKEN_CALLED" -> "CALLED";
            case "TOKEN_COMPLETED" -> "COMPLETED";
            case "TOKEN_CANCELLED" -> "CANCELLED";
            // For NO_SHOW, we'd need a specific event type, defaulting to CANCELLED logic or separate event
            default -> null;
        };
    }

    @Transactional(readOnly = true)
    public DailyStatsResponse getDailyStats(Long serviceId, LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        long totalTokens = analyticsRepository.countByServiceAndDate(serviceId, dateStr);
        long completed = analyticsRepository.countByServiceDateAndStatus(serviceId, dateStr, "COMPLETED");
        long cancelled = analyticsRepository.countByServiceDateAndStatus(serviceId, dateStr, "CANCELLED");
        long noShow = analyticsRepository.countByServiceDateAndStatus(serviceId, dateStr, "NO_SHOW");
        
        double completionRate = totalTokens > 0 ? ((double) completed / totalTokens) * 100 : 0.0;
        
        Double avgWaitOpt = analyticsRepository.avgWaitByServiceAndDate(serviceId, dateStr);
        double avgWait = avgWaitOpt != null ? avgWaitOpt : 0.0;
        
        List<Object[]> peakHourData = analyticsRepository.findPeakHourForDay(serviceId, dateStr);
        int peakHour = 0;
        long peakHourTokens = 0;
        
        if (!peakHourData.isEmpty()) {
            Object[] data = peakHourData.get(0);
            peakHour = data[0] != null ? ((Number) data[0]).intValue() : 0;
            peakHourTokens = data[1] != null ? ((Number) data[1]).longValue() : 0;
        }

        return DailyStatsResponse.builder()
                .serviceId(serviceId)
                .date(date)
                .totalTokens(totalTokens)
                .tokensCompleted(completed)
                .tokensCancelled(cancelled)
                .tokensNoShow(noShow)
                .completionRate(completionRate)
                .avgWaitSeconds(avgWait)
                .avgWaitDisplay(formatSeconds(avgWait))
                .peakHour(peakHour)
                .peakHourTokens(peakHourTokens)
                .build();
    }

    @Transactional(readOnly = true)
    public List<HourlyStatsResponse> getHourlyBreakdown(Long serviceId, LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<Object[]> results = analyticsRepository.hourlyBreakdown(serviceId, dateStr);
        
        return results.stream().map(row -> {
            int hour = row[0] != null ? ((Number) row[0]).intValue() : 0;
            long completed = row[1] != null ? ((Number) row[1]).longValue() : 0;
            long noShow = row[2] != null ? ((Number) row[2]).longValue() : 0;
            long cancelled = row[3] != null ? ((Number) row[3]).longValue() : 0;
            double avgWait = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            
            return HourlyStatsResponse.builder()
                    .serviceId(serviceId)
                    .hour(hour)
                    .tokensCompleted(completed)
                    .tokensNoShow(noShow)
                    .tokensCancelled(cancelled)
                    .avgWaitSeconds(avgWait)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceSummaryResponse getServiceSummary(Long serviceId, String serviceName) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        long waiting = analyticsRepository.countByServiceDateAndStatus(serviceId, today, "WAITING");
        long todayTotal = analyticsRepository.countByServiceAndDate(serviceId, today);
        long todayCompleted = analyticsRepository.countByServiceDateAndStatus(serviceId, today, "COMPLETED");
        
        Double todayAvgOpt = analyticsRepository.avgWaitByServiceAndDate(serviceId, today);
        double todayAvg = todayAvgOpt != null ? todayAvgOpt : 0.0;
        
        long weeklyTotal = analyticsRepository.weeklyTotal(serviceId);
        
        // Find peak day (simplified logic, actual implementation might group by date over a week)
        List<Object[]> peakHoursData = analyticsRepository.peakHours(serviceId);
        String peakDay = "None";
        if (!peakHoursData.isEmpty()) {
             // Basic grouping to find day with most tokens
             Map<Integer, Double> dayTokens = new HashMap<>();
             for (Object[] row : peakHoursData) {
                 int dow = row[1] != null ? ((Number) row[1]).intValue() : 0;
                 double tokens = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
                 dayTokens.merge(dow, tokens, Double::sum);
             }
             if (!dayTokens.isEmpty()) {
                 int maxDay = Collections.max(dayTokens.entrySet(), Map.Entry.comparingByValue()).getKey();
                 peakDay = DAY_NAMES[maxDay % 7];
             }
        }
        
        return ServiceSummaryResponse.builder()
                .serviceId(serviceId)
                .serviceName(serviceName)
                .currentWaiting(waiting)
                .todayTotal(todayTotal)
                .todayCompleted(todayCompleted)
                .todayAvgWaitSeconds(todayAvg)
                .weeklyTotal(weeklyTotal)
                .peakDay(peakDay)
                .build();
    }

    @Transactional(readOnly = true)
    public PeakHoursResponse getPeakHours(Long serviceId) {
        List<Object[]> results = analyticsRepository.peakHours(serviceId);
        
        List<PeakHoursResponse.HourlyBucket> buckets = results.stream().map(row -> {
            int dow = row[1] != null ? ((Number) row[1]).intValue() : 0;
            int hour = row[2] != null ? ((Number) row[2]).intValue() : 0;
            double avgTokens = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            double avgWait = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            
            return PeakHoursResponse.HourlyBucket.builder()
                    .dayOfWeek(dow)
                    .dayName(DAY_NAMES[dow % 7])
                    .hour(hour)
                    .avgTokens(avgTokens)
                    .avgWaitSeconds(avgWait)
                    .build();
        }).collect(Collectors.toList());
        
        return PeakHoursResponse.builder()
                .serviceId(serviceId)
                .data(buckets)
                .build();
    }
    
    private String formatSeconds(double totalSeconds) {
        if (totalSeconds <= 0) return "0 sec";
        long mins = (long) (totalSeconds / 60);
        long secs = (long) (totalSeconds % 60);
        if (mins > 0) {
            return mins + " min " + secs + " sec";
        } else {
            return secs + " sec";
        }
    }
}
