package com.smartqueue.queue.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SseEmitterService {

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emittersMap = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long serviceId) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 min timeout
        
        emittersMap.computeIfAbsent(serviceId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        emitter.onCompletion(() -> removeEmitter(serviceId, emitter));
        emitter.onTimeout(() -> removeEmitter(serviceId, emitter));
        emitter.onError(e -> removeEmitter(serviceId, emitter));
        
        try {
            emitter.send(SseEmitter.event().name("keepalive").data("connected"));
        } catch (IOException e) {
            log.error("Error sending initial keepalive for service {}", serviceId, e);
            removeEmitter(serviceId, emitter);
        }
        
        return emitter;
    }

    public void broadcastQueueUpdate(Long serviceId, Object eventData) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersMap.get(serviceId);
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("queue-update").data(eventData));
                } catch (IOException e) {
                    removeEmitter(serviceId, emitter);
                }
            }
        }
    }

    private void removeEmitter(Long serviceId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersMap.get(serviceId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersMap.remove(serviceId);
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    public void keepAlive() {
        emittersMap.forEach((serviceId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                } catch (IOException e) {
                    removeEmitter(serviceId, emitter);
                }
            }
        });
    }
}
