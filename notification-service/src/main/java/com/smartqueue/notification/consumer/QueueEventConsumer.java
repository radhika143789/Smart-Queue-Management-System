package com.smartqueue.notification.consumer;

import com.smartqueue.common.event.QueueEvent;
import com.smartqueue.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = {"token.booked", "token.called", "token.completed", "token.cancelled"},
                   groupId = "notification-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleQueueEvent(QueueEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received event: type={} tokenId={} topic={}", event.getEventType(), event.getTokenId(), topic);
        try {
            notificationService.processEvent(event);
        } catch (Exception e) {
            log.error("Error processing event {}: {}", event.getEventId(), e.getMessage(), e);
            // Event will be retried by Kafka consumer retry policy if configured, or DLQ.
            throw e;
        }
    }
}
