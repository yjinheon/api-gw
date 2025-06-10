package com.example.gw.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class ConditionalLoggingFilter extends AbstractGatewayFilterFactory<ConditionalLoggingFilter.Config> {

    public ConditionalLoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();

            // 설정된 경로 패턴과 매칭되는 경우에만 로깅
            boolean shouldLog = config.getPaths().stream()
                    .anyMatch(path -> request.getURI().getPath().startsWith(path));

            if (shouldLog) {
                log.info("[CONDITIONAL] {} {} - detailed logging enabled for this path",
                        request.getMethod(), request.getPath());

                // 요청 파라미터 상세 로깅
                if (!request.getQueryParams().isEmpty()) {
                    log.info("[CONDITIONAL] Query parameters: {}",
                            request.getQueryParams().toSingleValueMap());
                }
            }

            return chain.filter(exchange)
                    .doFinally(signal -> {
                        if (shouldLog) {
                            log.info("[CONDITIONAL] Response completed for {} {}",
                                    request.getMethod(), request.getPath());
                        }
                    });
        };
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("paths");
    }

    @Setter
    @Getter
    public static class Config {
        private List<String> paths;

    }
}
