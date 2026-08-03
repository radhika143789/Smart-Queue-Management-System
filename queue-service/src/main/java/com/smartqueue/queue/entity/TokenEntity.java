package com.smartqueue.queue.entity;

import com.smartqueue.common.enums.TokenStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "tokens", indexes = {
    @Index(name = "idx_tokens_service_status", columnList = "service_id, status"),
    @Index(name = "idx_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_tokens_booked_at", columnList = "booked_at"),
    @Index(name = "idx_tokens_service_date", columnList = "service_id, booked_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String tokenNumber;

    @Column(nullable = false)
    private int sequenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counter_id")
    private CounterEntity counter;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 255)
    private String userEmail;

    @Column(length = 20)
    private String userPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TokenStatus status = TokenStatus.WAITING;

    @Version
    @Builder.Default
    @Column(nullable = false)
    private Integer version = 0;

    @Column(nullable = false)
    private Instant bookedAt;

    private Instant calledAt;
    private Instant servedAt;
    private Instant completedAt;

    private Integer estimatedWaitSeconds;
    private Integer actualWaitSeconds;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
