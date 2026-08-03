package com.smartqueue.analytics.consumer;

import com.smartqueue.analytics.service.AnalyticsService;
import com.smartqueue.common.event.QueueEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for queue events to populate analytics database.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class QueueEventAnalyticsConsumer {

    private final AnalyticsService analyticsService;

    @KafkaListener(
            topics = {"token.booked", "token.called", "token.completed", "token.cancelled"},
            groupId = "analytics-service",
            containerFactory = "analyticsKafkaListenerContainerFactory"
    )
    public void handleEvent(QueueEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.debug("Received event on topic {}: {}", topic, event);
        try {
            analyticsService.upsertSnapshot(event);
        } catch (Exception e) {
            log.error("Error processing event for analytics: {}", event, e);
        }
    }
}
