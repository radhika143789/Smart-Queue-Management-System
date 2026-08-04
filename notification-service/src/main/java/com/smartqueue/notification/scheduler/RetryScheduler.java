package com.smartqueue.notification.scheduler;

import com.smartqueue.notification.entity.NotificationEntity;
import com.smartqueue.notification.provider.NotificationProvider;
import com.smartqueue.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RetryScheduler {

    private final NotificationRepository notificationRepository;
    private final List<NotificationProvider> providers;

    @Scheduled(fixedDelay = 300000) // every 5 minutes
    @Transactional
    public void retryFailedNotifications() {
        List<NotificationEntity> failed = notificationRepository
            .findByStatusAndRetryCountLessThan("FAILED", 3);

        log.info("RetryScheduler: found {} FAILED notifications to retry", failed.size());

        for (NotificationEntity notif : failed) {
            log.info("Retrying notification id={} type={} attempt={}", notif.getId(), notif.getType(), notif.getRetryCount() + 1);

            NotificationProvider provider = providers.stream()
                .filter(p -> p.supports(notif.getType()))
                .findFirst()
                .orElse(null);

            if (provider == null) {
                log.error("No provider found for type {} on retry — marking as DEAD", notif.getType());
                notif.setStatus("DEAD");
                notificationRepository.save(notif);
                continue;
            }

            notif.setRetryCount(notif.getRetryCount() + 1);

            // FIX (BUG #6): Use the stored body from the DB, not a generic placeholder.
            // NotificationService.sendNotification() now persists notif.body on first send.
            String body = notif.getBody();
            if (body == null || body.isBlank()) {
                log.warn("Notification id={} has no stored body — cannot retry meaningfully", notif.getId());
                notif.setStatus("DEAD");
                notif.setErrorMessage("No message body stored; retry not possible");
                notificationRepository.save(notif);
                continue;
            }

            try {
                provider.send(notif.getRecipient(), notif.getSubject(), body);
                notif.setStatus("SENT");
                notif.setSentAt(Instant.now());
                log.info("Successfully retried notification id={}", notif.getId());
            } catch (Exception e) {
                log.error("Failed to retry notification id={}: {}", notif.getId(), e.getMessage());
                notif.setErrorMessage(e.getMessage());
                if (notif.getRetryCount() >= 3) {
                    notif.setStatus("DEAD");
                    log.warn("Notification id={} exhausted retries — marked as DEAD", notif.getId());
                }
            }
            notificationRepository.save(notif);
        }
    }
}
