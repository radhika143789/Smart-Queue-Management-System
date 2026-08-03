package com.smartqueue.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entity representing a token snapshot for analytics.
 */
@Entity
@Table(
        name = "token_snapshots",
        indexes = {
                @Index(name = "idx_snapshots_service_booked", columnList = "service_id, booked_at DESC"),
                @Index(name = "idx_snapshots_service_status", columnList = "service_id, status"),
                @Index(name = "idx_snapshots_user", columnList = "user_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_id", unique = true, nullable = false)
    private Long tokenId;

    @Column(name = "token_number", length = 20)
    private String tokenNumber;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "WAITING";

    @Column(name = "booked_at")
    private Instant bookedAt;

    @Column(name = "called_at")
    private Instant calledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "estimated_wait_seconds")
    private Integer estimatedWaitSeconds;

    @Column(name = "actual_wait_seconds")
    private Integer actualWaitSeconds;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
