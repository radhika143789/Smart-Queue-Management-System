package com.smartqueue.queue.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MaterializedViewRefreshScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 900000) // every 15 min
    public void refreshMaterializedViews() {
        try {
            log.info("Starting refresh of materialized views");
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_hourly_queue_stats");
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_peak_hours");
            log.info("Successfully refreshed materialized views");
        } catch (Exception e) {
            log.error("Failed to refresh materialized views", e);
        }
    }
}
