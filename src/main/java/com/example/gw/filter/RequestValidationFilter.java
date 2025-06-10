package com.example.gw.filter;


// ===================================
// filter/RequestValidationFilter.java
// 요청의 유효성을 검사하는 필터 - 보안과 데이터 무결성 보장
// ===================================

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@Slf4j
public class RequestValidationFilter implements GlobalFilter, Ordered {

    // blocked user agent patterns
    private static final Set<String> BLOCKED_USER_AGENTS = Set.of(
            "bot", "crawler", "spider", "scraper"
    );

    // 최대 허용 URL 길이 (보안 공격 방지)
    private static final int MAX_URL_LENGTH = 2048;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. check url length
        if (request.getURI().toString().length() > MAX_URL_LENGTH) {
            log.warn("Blocked request with too long URL: {} characters",
                    request.getURI().toString().length());
            return createErrorResponse(exchange, "URL too long", HttpStatus.REQUEST_URI_TOO_LONG);
        }

        // 2. check User-Agent
        String userAgent = request.getHeaders().getFirst("User-Agent");
        if (userAgent != null && isBlockedUserAgent(userAgent)) {
            log.warn("Blocked request from suspicious User-Agent: {}", userAgent);
            return createErrorResponse(exchange, "Access denied", HttpStatus.FORBIDDEN);
        }

        // 3. check Content-Type (POST/PUT)
        if (needsContentTypeValidation(request.getMethod().name())) {
            String contentType = request.getHeaders().getFirst("Content-Type");
            if (contentType == null) {
                log.warn("Blocked {} request without Content-Type header", request.getMethod());
                return createErrorResponse(exchange, "Content-Type required", HttpStatus.BAD_REQUEST);
            }
        }

        // 4. check header ( API 버전 헤더)
        String apiVersion = request.getHeaders().getFirst("API-Version");
        if (isApiVersionRequired(request.getPath().value()) && apiVersion == null) {
            log.warn("Blocked request without API-Version header for path: {}", request.getPath());
            return createErrorResponse(exchange, "API-Version header required", HttpStatus.BAD_REQUEST);
        }

        // if all passed
        log.debug("Request validation passed for {}", request.getPath());
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 검증은 가능한 빨리 수행되어야 하므로 높은 우선순위
        return -2;
    }

    // check User-Agent
    private boolean isBlockedUserAgent(String userAgent) {
        String lowerUserAgent = userAgent.toLowerCase();
        return BLOCKED_USER_AGENTS.stream()
                .anyMatch(lowerUserAgent::contains);
    }

    // check if Content Type Validation is needed
    private boolean needsContentTypeValidation(String method) {
        return Set.of("POST", "PUT", "PATCH").contains(method);
    }

    // check if api version header is needed
    private boolean isApiVersionRequired(String path) {
        return path.startsWith("/api/v") || path.startsWith("/api/public");
    }

    // create appropriate error response
    private Mono<Void> createErrorResponse(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // JSON error response body
        String body = String.format("""
            {
                "error": "%s",
                "status": %d,
                "timestamp": "%s",
                "path": "%s"
            }
            """,
                message,
                status.value(),
                java.time.Instant.now(),
                exchange.getRequest().getPath()
        );

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
