package com.smartqueue.queue.service;

import com.smartqueue.common.enums.TokenStatus;
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
                .orElseThrow(() -> new RuntimeException("Service not found or inactive"));

        Optional<TokenEntity> activeToken = tokenRepository.findByUserIdAndServiceIdAndStatusIn(
                userId, serviceId, List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING));
        if (activeToken.isPresent()) {
            throw new RuntimeException("ACTIVE_TOKEN_EXISTS");
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

        redisTemplate.opsForZSet().add(QUEUE_KEY_PREFIX + serviceId, String.valueOf(token.getId()), newSeq);
        redisTemplate.expire(QUEUE_KEY_PREFIX + serviceId, 24, TimeUnit.HOURS);

        kafkaProducerService.publishEvent("token.booked", token);

        return mapToTokenResponse(token, position, eta);
    }

    public QueueStatusResponse getQueueStatus(Long serviceId, Long userId) {
        ServiceEntity service = serviceRepository.findByIdAndIsActiveTrue(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

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

    public TokenResponse getTokenDetails(Long tokenId, Long userId) {
        TokenEntity token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        if (userId != null && !token.getUserId().equals(userId)) {
            // Check roles later in controller, for now throw
            throw new RuntimeException("Unauthorized");
        }
        
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY_PREFIX + token.getService().getId(), String.valueOf(tokenId));
        int pos = rank != null ? rank.intValue() + 1 : 0;
        int eta = etaCalculationService.calculateEtaSeconds(pos, token.getService().getAvgServiceTimeSeconds());
        
        return mapToTokenResponse(token, pos, eta);
    }

    @Transactional
    public TokenResponse cancelToken(Long tokenId, Long userId) {
        TokenEntity token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        if (!token.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        if (token.getStatus() != TokenStatus.WAITING) {
            throw new RuntimeException("Cannot cancel token not in WAITING state");
        }
        
        token.setStatus(TokenStatus.CANCELLED);
        tokenRepository.save(token);
        
        redisTemplate.opsForZSet().remove(QUEUE_KEY_PREFIX + token.getService().getId(), String.valueOf(tokenId));
        
        kafkaProducerService.publishEvent("token.cancelled", token);
        
        return mapToTokenResponse(token, 0, 0);
    }

    @Transactional
    public CallNextResponse callNextToken(Long serviceId, Long counterId, Long staffUserId) {
        Set<ZSetOperations.TypedTuple<String>> tops = redisTemplate.opsForZSet().popMin(QUEUE_KEY_PREFIX + serviceId, 1);
        if (tops == null || tops.isEmpty()) {
            throw new RuntimeException("Queue is empty");
        }
        
        String tokenIdStr = tops.iterator().next().getValue();
        TokenEntity token = tokenRepository.findById(Long.valueOf(tokenIdStr))
                .orElseThrow(() -> new RuntimeException("Token not found"));
                
        CounterEntity counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Counter not found"));
                
        token.setStatus(TokenStatus.CALLED);
        token.setCounter(counter);
        token.setCalledAt(Instant.now());
        tokenRepository.save(token);
        
        redisTemplate.opsForValue().set(CURRENT_KEY_PREFIX + serviceId, token.getTokenNumber(), 1, TimeUnit.HOURS);
        
        Long remaining = redisTemplate.opsForZSet().zCard(QUEUE_KEY_PREFIX + serviceId);
        
        kafkaProducerService.publishEvent("token.called", token);
        sseEmitterService.broadcastQueueUpdate(serviceId, "Called " + token.getTokenNumber());
        
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
                .orElseThrow(() -> new RuntimeException("Token not found"));
        if (token.getStatus() != TokenStatus.SERVING && token.getStatus() != TokenStatus.CALLED) {
            throw new RuntimeException("Token is not CALLED or SERVING");
        }
        
        token.setStatus(TokenStatus.COMPLETED);
        token.setCompletedAt(Instant.now());
        
        if (token.getCalledAt() != null) {
            int duration = (int) (token.getCompletedAt().getEpochSecond() - token.getCalledAt().getEpochSecond());
            token.setActualWaitSeconds(duration);
            etaCalculationService.updateRollingAvgServiceTime(token.getService(), duration);
        }
        
        tokenRepository.save(token);
        kafkaProducerService.publishEvent("token.completed", token);
        
        return mapToTokenResponse(token, 0, 0);
    }

    @Transactional
    public TokenResponse markNoShow(Long tokenId) {
        TokenEntity token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        
        token.setStatus(TokenStatus.NO_SHOW);
        tokenRepository.save(token);
        kafkaProducerService.publishEvent("token.cancelled", token);
        
        return mapToTokenResponse(token, 0, 0);
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
