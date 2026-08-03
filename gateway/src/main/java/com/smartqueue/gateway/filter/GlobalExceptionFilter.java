package com.smartqueue.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class GlobalExceptionFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).onErrorResume(throwable -> {
            log.error("Gateway error: {}", throwable.getMessage(), throwable);
            ServerHttpResponse response = exchange.getResponse();
            
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            String message = "Internal Server Error";
            String code = "GATEWAY_500";

            if (throwable instanceof ResponseStatusException) {
                status = (HttpStatus) ((ResponseStatusException) throwable).getStatusCode();
                message = ((ResponseStatusException) throwable).getReason();
                code = "GATEWAY_STATUS_" + status.value();
            } else if (throwable instanceof TimeoutException) {
                status = HttpStatus.GATEWAY_TIMEOUT;
                message = "Gateway Timeout";
                code = "GATEWAY_504";
            }

            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String json = String.format("{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}", code, message);
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
