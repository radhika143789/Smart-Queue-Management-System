package com.smartqueue.notification.service;

import com.smartqueue.common.event.QueueEvent;
import com.smartqueue.notification.entity.NotificationEntity;
import com.smartqueue.notification.provider.NotificationProvider;
import com.smartqueue.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final List<NotificationProvider> providers;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void processEvent(QueueEvent event) {
        if (event == null || event.getEventType() == null) {
            log.warn("Received null event or event type");
            return;
        }

        switch (event.getEventType()) {
            case "TOKEN_BOOKED":
                sendBookingConfirmation(event);
                break;
            case "TOKEN_CALLED":
                sendCalledAlert(event);
                break;
            case "TOKEN_COMPLETED":
                sendCompletionReceipt(event);
                break;
            case "TOKEN_CANCELLED":
                sendCancellationConfirmation(event);
                break;
            default:
                log.info("Unhandled event type: {}", event.getEventType());
        }
    }

    private void sendBookingConfirmation(QueueEvent event) {
        String subject = "Your Queue Token #" + event.getTokenNumber() + " - Confirmed";
        
        String htmlBody = String.format("<html><body>" +
                "<h3>Booking Confirmation</h3>" +
                "<p>Your token number is <strong>#%s</strong> for service <strong>%s</strong>.</p>" +
                "<p>Estimated wait time: ~%d min.</p>" +
                "</body></html>",
                event.getTokenNumber(), event.getServiceName(), event.getEstimatedWaitMinutes());

        if (event.getUserEmail() != null && !event.getUserEmail().isEmpty()) {
            sendNotification("EMAIL", event.getUserEmail(), subject, htmlBody, event);
        }

        if (event.getUserPhone() != null && !event.getUserPhone().isEmpty()) {
            String smsBody = String.format("Your token #%s for %s is confirmed. Estimated wait: ~%d min.",
                    event.getTokenNumber(), event.getServiceName(), event.getEstimatedWaitMinutes());
            sendNotification("SMS", event.getUserPhone(), null, smsBody, event);
        }
    }

    private void sendCalledAlert(QueueEvent event) {
        if (event.getUserPhone() != null && !event.getUserPhone().isEmpty()) {
            String smsBody = String.format("URGENT: Token #%s - Please proceed to %s NOW.",
                    event.getTokenNumber(), event.getCounterName() != null ? event.getCounterName() : "Counter");
            sendNotification("SMS", event.getUserPhone(), null, smsBody, event);
        }

        if (event.getUserEmail() != null && !event.getUserEmail().isEmpty()) {
            String subject = "Your turn!";
            String body = String.format("<html><body><h3>Your turn!</h3><p>Proceed to %s.</p></body></html>",
                    event.getCounterName() != null ? event.getCounterName() : "Counter");
            sendNotification("EMAIL", event.getUserEmail(), subject, body, event);
        }
    }

    private void sendCompletionReceipt(QueueEvent event) {
        if (event.getUserEmail() != null && !event.getUserEmail().isEmpty()) {
            String subject = "Service Completed";
            String body = "<html><body><h3>Service completed.</h3><p>Thank you for using SmartQueue.</p></body></html>";
            sendNotification("EMAIL", event.getUserEmail(), subject, body, event);
        }
    }

    private void sendCancellationConfirmation(QueueEvent event) {
        String subject = "Token Cancelled";
        String bodyText = String.format("Your token #%s has been cancelled.", event.getTokenNumber());
        
        if (event.getUserEmail() != null && !event.getUserEmail().isEmpty()) {
            String htmlBody = "<html><body><p>" + bodyText + "</p></body></html>";
            sendNotification("EMAIL", event.getUserEmail(), subject, htmlBody, event);
        }

        if (event.getUserPhone() != null && !event.getUserPhone().isEmpty()) {
            sendNotification("SMS", event.getUserPhone(), null, bodyText, event);
        }
    }

    public void sendNotification(String type, String recipient, String subject, String body, QueueEvent event) {
        NotificationProvider provider = providers.stream()
                .filter(p -> p.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider found for type: " + type));

        NotificationEntity notification = NotificationEntity.builder()
                .type(type)
                .recipient(recipient)
                .subject(subject)
                .body(body)   // FIX (BUG #6): persist body so RetryScheduler can re-send actual content
                // referenceId is String — convert tokenId to String for tracing
                .referenceId(event.getTokenId() != null
                        ? String.valueOf(event.getTokenId())
                        : event.getEventId())
                .eventType(event.getEventType())
                .status("PENDING")
                .build();
        
        notificationRepository.save(notification);

        try {
            provider.send(recipient, subject, body);
            notification.setStatus("SENT");
            notification.setSentAt(Instant.now());
        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
            log.error("Failed to send notification via {}: {}", type, e.getMessage());
        } finally {
            notificationRepository.save(notification);
        }
    }
}
