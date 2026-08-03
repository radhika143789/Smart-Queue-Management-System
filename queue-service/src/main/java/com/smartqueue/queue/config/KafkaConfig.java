package com.smartqueue.queue.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public NewTopic tokenBookedTopic() {
        return TopicBuilder.name("token.booked")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tokenCalledTopic() {
        return TopicBuilder.name("token.called")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tokenCompletedTopic() {
        return TopicBuilder.name("token.completed")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tokenCancelledTopic() {
        return TopicBuilder.name("token.cancelled")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
