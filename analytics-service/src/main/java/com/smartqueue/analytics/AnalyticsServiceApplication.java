package com.smartqueue.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Analytics Service.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@ComponentScan(basePackages = {"com.smartqueue.analytics", "com.smartqueue.common"})
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
