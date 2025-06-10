package com.example.gw.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 1st route: /api/test/** -> httpbin.org (for testing)
                .route("test-route", r -> r
                        .path("/api/test/**")
                        .filters(f -> f
                                .stripPrefix(2) // "/api/test" 제거하고 뒤의 경로만 전달
                                .addRequestHeader("X-Gateway-Route", "advanced-test")
                                .addRequestHeader("X-Processing-Node", "gateway-node-1")
                                .addResponseHeader("X-Processed-By", "Spring-Cloud-Gateway")
                                .retry(3)
                        ).uri("https://httpbin.org")
                )
                //  2nd route: /api/echo/** -> echo server
                .route("echo-route", r -> r
                        .path("/api/echo/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Custom-Header", "From-Gateway")
                        ).uri("https://postman-echo.com")
                )
                // 3rd route: default route (fallback)
                .route("default-route", r -> r
                        .path("/**")
                        .filters(f -> f
                                .addResponseHeader("X-Gateway-Default", "true")
                        ).uri("https://httpbin.org"))

                // test: fast response
                .route("fast-test-route", r ->
                        r.path("/api/fast/**")
                                .filters(f ->
                                        f.stripPrefix(2)
                                                .addRequestHeader("X-Test-Type", "fast-response")
                                                .circuitBreaker(config -> config
                                                        .setName("fast-test-cb")
                                                        .setFallbackUri("/gateway/fallback")
                                                )
                                ).uri("https://httpbin.org")
                )

                // 에러 테스트용 라우팅
                .route("error-test-route", r -> r
                        .path("/api/error/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Test-Type", "error-handling")
                        )
                        .uri("https://httpbin.org/status/500"))
                .build();
    }

}



