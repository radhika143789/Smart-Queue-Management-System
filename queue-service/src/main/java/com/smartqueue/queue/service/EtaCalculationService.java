package com.smartqueue.queue.service;

import com.smartqueue.common.enums.TokenStatus;
import com.smartqueue.queue.entity.ServiceEntity;
import com.smartqueue.queue.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EtaCalculationService {

    private final ServiceRepository serviceRepository;

    public int calculateEtaSeconds(int queuePosition, int avgServiceTimeSeconds) {
        if (queuePosition <= 0) return 0;
        return queuePosition * avgServiceTimeSeconds;
    }

    public String formatEtaDisplay(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "Next up!";
        }
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        
        if (hours > 0) {
            return String.format("~%d hr %d min", hours, minutes);
        } else if (minutes > 0) {
            return String.format("~%d min", minutes);
        } else {
            return "Less than a minute";
        }
    }

    @Transactional
    public void updateRollingAvgServiceTime(ServiceEntity service, int actualDurationSeconds) {
        if (actualDurationSeconds <= 0) return;
        
        double alpha = 0.1;
        int currentAvg = service.getAvgServiceTimeSeconds();
        int newAvg = (int) Math.round((alpha * actualDurationSeconds) + ((1 - alpha) * currentAvg));
        
        service.setAvgServiceTimeSeconds(newAvg);
        serviceRepository.save(service);
        log.debug("Updated avg service time for service {} to {} seconds", service.getId(), newAvg);
    }
}
