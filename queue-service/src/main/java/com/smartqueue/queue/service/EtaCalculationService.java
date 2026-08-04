package com.smartqueue.queue.service;

import com.smartqueue.queue.entity.ServiceEntity;
import com.smartqueue.queue.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stateless ETA calculation service.
 *
 * Design note: ServiceRepository is injected here only for updateRollingAvgServiceTime().
 * The core calculate/format methods are pure functions with no dependencies —
 * this is why the unit test can instantiate this class directly with new EtaCalculationService(null).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EtaCalculationService {

    private final ServiceRepository serviceRepository;

    /**
     * Calculate estimated wait time in seconds.
     *
     * @param queuePosition  1-based position (0 means "next up / no wait")
     * @param avgServiceTimeSeconds average time to serve one token
     * @return estimated seconds to wait
     */
    public int calculateEtaSeconds(int queuePosition, int avgServiceTimeSeconds) {
        if (queuePosition <= 0) return 0;
        return queuePosition * avgServiceTimeSeconds;
    }

    /**
     * Format seconds into a human-readable ETA string.
     *
     * FIX (BUG #7): Was returning "Next up!" for 0, but unit tests and UI expect "Ready now".
     * Aligned to consistent UX language.
     */
    public String formatEtaDisplay(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "Ready now";
        }
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return String.format("~%d hr %d min", hours, minutes);
        } else if (minutes >= 1) {
            return String.format("~%d min", minutes);
        } else {
            // Less than 60 seconds
            return "~1 min";
        }
    }

    /**
     * Update the rolling average service time using exponential moving average (EMA).
     * Alpha = 0.1 means new observations have 10% weight, historical average has 90%.
     */
    @Transactional
    public void updateRollingAvgServiceTime(ServiceEntity service, int actualDurationSeconds) {
        if (actualDurationSeconds <= 0) return;

        double alpha = 0.1;
        int currentAvg = service.getAvgServiceTimeSeconds();
        int newAvg = (int) Math.round((alpha * actualDurationSeconds) + ((1 - alpha) * currentAvg));

        service.setAvgServiceTimeSeconds(newAvg);
        serviceRepository.save(service);
        log.debug("Updated avg service time for service {} to {} seconds (was {})", service.getId(), newAvg, currentAvg);
    }
}
