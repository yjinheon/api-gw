package com.example.gw.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ResponseTimeFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // pre-filter
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange)
                .doFinally(signal -> {
                    // post-filter
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    // 응답 헤더에 처리 시간 추가 - 클라이언트가 성능을 모니터링할 수 있게 함
                    exchange.getResponse().getHeaders().add("X-Response-Time", duration + "ms");
                    exchange.getResponse().getHeaders().add("X-Gateway-Processing-Time", duration + "ms");
                    if (duration > 1000) {
                        // 1초 이상 걸린 요청은 경고 로그
                        log.warn("Slow request detected: {} {} took {}ms",
                                exchange.getRequest().getMethod(),
                                exchange.getRequest().getPath(),
                                duration
                        );
                    } else {
                        log.debug("Request processed in {}ms", duration);
                    }

                    // if requestId is not null add to log
                    String requestId = exchange.getAttribute("requestId");
                    if (requestId != null) {
                        log.info("[PERF-{}] Total processing time: {}ms", requestId, duration);
                    }
                });
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
