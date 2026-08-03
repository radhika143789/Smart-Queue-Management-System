package com.smartqueue.notification.scheduler;

import com.smartqueue.notification.entity.NotificationEntity;
import com.smartqueue.notification.provider.NotificationProvider;
import com.smartqueue.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RetryScheduler {

    private final NotificationRepository notificationRepository;
    private final List<NotificationProvider> providers;

    @Scheduled(fixedDelay = 300000) // every 5 minutes
    public void retryFailedNotifications() {
        List<NotificationEntity> failed = notificationRepository
            .findByStatusAndRetryCountLessThan("FAILED", 3);
        
        for (NotificationEntity notif : failed) {
            log.info("Retrying notification id={} attempt={}", notif.getId(), notif.getRetryCount() + 1);
            
            NotificationProvider provider = providers.stream()
                .filter(p -> p.supports(notif.getType()))
                .findFirst()
                .orElse(null);

            if (provider == null) {
                log.error("No provider found for type {} on retry", notif.getType());
                continue;
            }

            notif.setRetryCount(notif.getRetryCount() + 1);

            try {
                // For retry, we don't store the exact body in DB in this version,
                // so an advanced implementation would re-construct it or store it.
                // Assuming we stored 'subject' and if not, we send a generic fallback.
                provider.send(notif.getRecipient(), notif.getSubject(), 
                        "This is a retried message for event " + notif.getEventType());
                
                notif.setStatus("SENT");
                notif.setSentAt(Instant.now());
                log.info("Successfully retried notification id={}", notif.getId());
            } catch (Exception e) {
                log.error("Failed to retry notification id={}: {}", notif.getId(), e.getMessage());
                if (notif.getRetryCount() >= 3) {
                    notif.setStatus("DEAD");
                }
                notif.setErrorMessage(e.getMessage());
            }
            notificationRepository.save(notif);
        }
    }
}
