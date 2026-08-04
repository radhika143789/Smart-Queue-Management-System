package com.smartqueue.queue.service;

import com.smartqueue.common.enums.ErrorCode;
import com.smartqueue.common.enums.TokenStatus;
import com.smartqueue.common.event.QueueEvent;
import com.smartqueue.common.exception.AppException;
import com.smartqueue.queue.dto.CallNextResponse;
import com.smartqueue.queue.dto.QueueStatusResponse;
import com.smartqueue.queue.dto.TokenResponse;
import com.smartqueue.queue.entity.CounterEntity;
import com.smartqueue.queue.entity.ServiceEntity;
import com.smartqueue.queue.entity.TokenEntity;
import com.smartqueue.queue.repository.CounterRepository;
import com.smartqueue.queue.repository.ServiceRepository;
import com.smartqueue.queue.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueService {

    private final TokenRepository tokenRepository;
    private final ServiceRepository serviceRepository;
    private final CounterRepository counterRepository;
    private final StringRedisTemplate redisTemplate;
    private final EtaCalculationService etaCalculationService;
    private final SseEmitterService sseEmitterService;
    private final KafkaProducerService kafkaProducerService;

    private static final String QUEUE_KEY_PREFIX = "queue:";
    private static final String CURRENT_KEY_PREFIX = "current:";
    private static final String[] PREFIX_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");

    @Transactional
    public TokenResponse bookToken(Long serviceId, Long userId, String userEmail, String userPhone) {
        ServiceEntity service = serviceRepository.findByIdAndIsActiveTrue(serviceId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));

        Optional<TokenEntity> activeToken = tokenRepository.findByUserIdAndServiceIdAndStatusIn(
                userId, serviceId, List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING));
        if (activeToken.isPresent()) {
            throw new AppException(ErrorCode.ACTIVE_TOKEN_EXISTS);
        }

        int maxSeq = tokenRepository.findMaxSequenceForToday(serviceId);
        int newSeq = maxSeq + 1;
        String prefix = PREFIX_LETTERS[(int) (serviceId % 26)];
        String tokenNumber = String.format("%s-%03d", prefix, newSeq);

        Long queueSize = redisTemplate.opsForZSet().zCard(QUEUE_KEY_PREFIX + serviceId);
        int position = queueSize != null ? queueSize.intValue() + 1 : 1;
        int eta = etaCalculationService.calculateEtaSeconds(position, service.getAvgServiceTimeSeconds());

        TokenEntity token = TokenEntity.builder()
                .tokenNumber(tokenNumber)
                .sequenceNumber(newSeq)
                .service(service)
                .userId(userId)
                .userEmail(userEmail)
                .userPhone(userPhone)
                .status(TokenStatus.WAITING)
                .bookedAt(Instant.now())
                .estimatedWaitSeconds(eta)
                .build();
        token = tokenRepository.save(token);

        // Push to Redis ZSET — score = sequence so ZPOPMIN always gets the oldest token
        redisTemplate.opsForZSet().add(QUEUE_KEY_PREFIX + serviceId, String.valueOf(token.getId()), newSeq);
        redisTemplate.expire(QUEUE_KEY_PREFIX + serviceId, 24, TimeUnit.HOURS);

        // Build and publish typed QueueEvent (never pass raw entity to Kafka)
        QueueEvent event = buildEvent("TOKEN_BOOKED", token, null, position, eta);
        kafkaProducerService.publishEvent("token.booked", event);

        return mapToTokenResponse(token, position, eta);
    }

    @Transactional(readOnly = true)
    public QueueStatusResponse getQueueStatus(Long serviceId, Long userId) {
        ServiceEntity service = serviceRepository.findByIdAndIsActiveTrue(serviceId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));

        Long queueSize = redisTemplate.opsForZSet().zCard(QUEUE_KEY_PREFIX + serviceId);
        int waiting = queueSize != null ? queueSize.intValue() : 0;
        
        String currentTokenNumber = redisTemplate.opsForValue().get(CURRENT_KEY_PREFIX + serviceId);

        TokenResponse myToken = null;
        if (userId != null) {
            Optional<TokenEntity> active = tokenRepository.findByUserIdAndServiceIdAndStatusIn(
                    userId, serviceId, List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING));
            if (active.isPresent()) {
                TokenEntity t = active.get();
                Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY_PREFIX + serviceId, String.valueOf(t.getId()));
                int pos = rank != null ? rank.intValue() + 1 : 0;
                int eta = etaCalculationService.calculateEtaSeconds(pos, service.getAvgServiceTimeSeconds());
                myToken = mapToTokenResponse(t, pos, eta);
            }
        }

        return QueueStatusResponse.builder()
                .serviceId(serviceId)
                .serviceName(service.getName())
                .isOpen(service.isActive())
                .currentlyServing(currentTokenNumber)
                .totalWaiting(waiting)
                .estimatedWaitForNextSeconds(etaCalculationService.calculateEtaSeconds(1, service.getAvgServiceTimeSeconds()))
                .myToken(myToken)
                .build();
    }

    @Transactional(readOnly = true)
    public TokenResponse getTokenDetails(Long tokenId, Long userId) {
        TokenEntity token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_NOT_FOUND));
        if (userId != null && !token.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY_PREFIX + token.getService().getId(), String.valueOf(tokenId));
        int pos = rank != null ? rank.intValue() + 1 : 0;
        int eta = etaCalculationService.calculateEtaSeconds(pos, token.getService().getAvgServiceTimeSeconds());
        
        return mapToTokenResponse(token, pos, eta);
    }

    @Transactional
    public TokenResponse cancelToken(Long tokenId, Long userId) {
        TokenEntity token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_NOT_FOUND));
        if (!token.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (token.getStatus() != TokenStatus.WAITING && token.getStatus() != TokenStatus.CALLED) {
            throw new AppException(ErrorCode.INVALID_TOKEN_OPERATION,
                    "Only WAITING or CALLED tokens can be cancelled. Current status: " + token.getStatus());
        }

        token.setStatus(TokenStatus.CANCELLED);
        tokenRepository.save(token);

        redisTemplate.opsForZSet().remove(QUEUE_KEY_PREFIX + token.getService().getId(), String.valueOf(tokenId));

        QueueEvent event = buildEvent("TOKEN_CANCELLED", token, null, 0, 0);
        kafkaProducerService.publishEvent("token.cancelled", event);

        return mapToTokenResponse(token, 0, 0);
    }

    @Transactional
    public CallNextResponse callNextToken(Long serviceId, Long counterId, Long staffUserId) {
        // FIX (BUG #3): Peek at the next token BEFORE popping, so we can validate DB first.
        // Old code: popMin first → if DB throws, token is PERMANENTLY lost from Redis queue.
        Set<ZSetOperations.TypedTuple<String>> tops = redisTemplate.opsForZSet().rangeWithScores(QUEUE_KEY_PREFIX + serviceId, 0, 0);
        if (tops == null || tops.isEmpty()) {
            throw new AppException(ErrorCode.QUEUE_CLOSED, "No tokens waiting in queue");
        }

        String tokenIdStr = tops.iterator().next().getValue();
        TokenEntity token = tokenRepository.findById(Long.valueOf(tokenIdStr))
                .orElseThrow(() -> {
                    // Token in Redis but not in DB — stale entry. Remove it and report.
                    redisTemplate.opsForZSet().remove(QUEUE_KEY_PREFIX + serviceId, tokenIdStr);
                    log.error("Stale Redis entry for tokenId={} in service={} — removed", tokenIdStr, serviceId);
                    return new AppException(ErrorCode.TOKEN_NOT_FOUND);
                });

        CounterEntity counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Counter not found: " + counterId));

        // DB update FIRST — only then remove from Redis
        token.setStatus(TokenStatus.CALLED);
        token.setCounter(counter);
        token.setCalledAt(Instant.now());
        tokenRepository.save(token);

        // Safe to pop now — DB is committed
        redisTemplate.opsForZSet().remove(QUEUE_KEY_PREFIX + serviceId, tokenIdStr);
        redisTemplate.opsForValue().set(CURRENT_KEY_PREFIX + serviceId, token.getTokenNumber(), 1, TimeUnit.HOURS);

        Long remaining = redisTemplate.opsForZSet().zCard(QUEUE_KEY_PREFIX + serviceId);

        QueueEvent event = buildEvent("TOKEN_CALLED", token, counter.getName(), 0, 0);
        kafkaProducerService.publishEvent("token.called", event);
        sseEmitterService.broadcastQueueUpdate(serviceId, event);

        return CallNextResponse.builder()
                .calledTokenNumber(token.getTokenNumber())
                .userId(token.getUserId())
                .counterName(counter.getName())
                .queueRemaining(remaining != null ? remaining.intValue() : 0)
                .build();
    }

    @Transactional
    public TokenResponse completeToken(Long tokenId, Long staffUserId) {
        TokenEntity token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_NOT_FOUND));
        if (token.getStatus() != TokenStatus.SERVING && token.getStatus() != TokenStatus.CALLED) {
            throw new AppException(ErrorCode.INVALID_TOKEN_OPERATION,
                    "Token must be in CALLED or SERVING state to complete. Current: " + token.getStatus());
        }

        token.setStatus(TokenStatus.COMPLETED);
        token.setCompletedAt(Instant.now());

        if (token.getCalledAt() != null) {
            int duration = (int) (token.getCompletedAt().getEpochSecond() - token.getCalledAt().getEpochSecond());
            token.setActualWaitSeconds(duration);
            etaCalculationService.updateRollingAvgServiceTime(token.getService(), duration);
        }

        tokenRepository.save(token);

        QueueEvent event = buildEvent("TOKEN_COMPLETED", token, null, 0, 0);
        kafkaProducerService.publishEvent("token.completed", event);

        return mapToTokenResponse(token, 0, 0);
    }

    @Transactional
    public TokenResponse markNoShow(Long tokenId) {
        TokenEntity token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_NOT_FOUND));

        if (token.getStatus() != TokenStatus.CALLED) {
            throw new AppException(ErrorCode.INVALID_TOKEN_OPERATION,
                    "Only CALLED tokens can be marked as NO_SHOW. Current status: " + token.getStatus());
        }

        token.setStatus(TokenStatus.NO_SHOW);
        tokenRepository.save(token);

        // FIX (BUG): Publish TOKEN_NO_SHOW (not TOKEN_CANCELLED) so analytics and notification
        // services can distinguish no-shows from deliberate cancellations.
        QueueEvent event = buildEvent("TOKEN_NO_SHOW", token, null, 0, 0);
        kafkaProducerService.publishEvent("token.cancelled", event);

        return mapToTokenResponse(token, 0, 0);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Build a typed QueueEvent from a TokenEntity. Always use this instead of
     * passing raw entities to Kafka — keeps inter-service contract explicit.
     */
    private QueueEvent buildEvent(String type, TokenEntity token, String counterName, int position, int etaSeconds) {
        return QueueEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(type)
                .tokenId(token.getId())
                .tokenNumber(token.getTokenNumber())
                .serviceId(token.getService().getId())
                .serviceName(token.getService().getName())
                .userId(token.getUserId())
                .userEmail(token.getUserEmail())
                .userPhone(token.getUserPhone())
                .oldStatus(null)
                .newStatus(token.getStatus())
                .queuePosition(position)
                .estimatedWaitSeconds(etaSeconds)
                .counterName(counterName)
                .occurredAt(Instant.now())
                .build();
    }

    private TokenResponse mapToTokenResponse(TokenEntity token, int position, int etaSeconds) {
        return TokenResponse.builder()
                .tokenId(token.getId())
                .tokenNumber(token.getTokenNumber())
                .serviceName(token.getService().getName())
                .status(token.getStatus())
                .queuePosition(position)
                .estimatedWaitSeconds(etaSeconds)
                .estimatedWaitDisplay(etaCalculationService.formatEtaDisplay(etaSeconds))
                .counterName(token.getCounter() != null ? token.getCounter().getName() : null)
                .bookedAt(token.getBookedAt())
                .calledAt(token.getCalledAt())
                .build();
    }
}
