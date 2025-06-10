package com.example.gw.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
logging
 */

@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {



    @Override
    public int getOrder() {
        // give top priority
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = generateRequestId();

        log.info("[{}][Request: {} ] {} {} from {}"
                , LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
                , requestId
                , request.getMethod()
                , request.getPath()
                , getClientIP(exchange));

        log.debug("[{}][Request : {} ] headers : {}"
                , LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
                , requestId
                , request.getHeaders().toSingleValueMap());

        if (!request.getQueryParams().isEmpty()) {
            log.debug("[REQUEST-{}] Query Params: {}", requestId, request.getQueryParams().toSingleValueMap());
        }
        // put requestId into exchange
        exchange.getAttributes().put("requestId", requestId);
        exchange.getAttributes().put("requestStartTime", System.currentTimeMillis());

        return chain.filter(exchange)
                .doOnSuccess(aVoid -> {
                    // define post-filter logic
                    ServerHttpResponse response = exchange.getResponse();
                    long startTime = (Long) exchange.getAttributes().get("requestStartTime");
                    long duration = System.currentTimeMillis() - startTime;

                    log.info("[{}][Response : {} ] Status : {} Duration: {} ms"
                            , LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
                            , requestId
                            , response.getStatusCode()
                            , duration);
                })
                .doOnError(throwable -> {
                    // 에러 발생 시 로깅
                    long startTime =  exchange.getAttribute("requestStartTime");
                    long duration = System.currentTimeMillis() - startTime;

                    log.error("[ERROR-{}] Failed after {}ms: {}",
                            requestId, duration, throwable.getMessage(), throwable);
                });

    }


    private String getClientIP(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. check X-forwarded-for header
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // 2. check X-Real-IP  header
        String xRealIP = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

    }

    // generate unique id per request
    private String generateRequestId() {
        return String.valueOf(System.currentTimeMillis() % 100000) +
                String.valueOf((int)(Math.random() * 1000));
    }
}
