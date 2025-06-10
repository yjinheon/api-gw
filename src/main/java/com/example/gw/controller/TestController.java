package com.example.gw.controller;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class TestController {

    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {

        Map<String, Object> res = new HashMap<>();
        res.put("status", "UP");
        res.put("timestamp", LocalDateTime.now());
        res.put("service","API Gateway");
        res.put("version","1.0.0");
        res.put("filters","Custom filters enabled");

        return Mono.just(res);

    }


    @GetMapping("/info")
    public Mono<Map<String, Object>> info() {
        Map<String, Object> res = new HashMap<>();
        res.put("gateway", "Spring Cloud Gateway");
        res.put("routes", "5 routes configured");
        res.put("customFilters", Map.of(
                "RequestLoggingFilter", "Logs all requests and responses",
                "ResponseTimeFilter", "Measures and reports processing time",
                "RequestValidationFilter", "Validates incoming requests"
        ));
        return Mono.just(res);
    }

    @RequestMapping("/fallback")
    public Mono<Map<String, Object>> fallback() {
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Service temporarily unavailable");
        res.put("fallback", true);
        res.put("timestamp", LocalDateTime.now());
        res.put("suggestion", "Please try again later");
        return Mono.just(res);
    }

    // 필터 테스트용 엔드포인트들
    @PostMapping("/test/validation")
    public Mono<Map<String, Object>> testValidation(@RequestBody(required = false) String body) {
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Validation test endpoint");
        res.put("bodyReceived", body != null);
        res.put("timestamp", LocalDateTime.now());
        return Mono.just(res);
    }

    @GetMapping("/test/slow")
    public Mono<Map<String, Object>> slowEndpoint() {
        // 의도적으로 느린 응답 생성
        return Mono.delay(java.time.Duration.ofSeconds(2))
                .map(delay -> {
                    Map<String, Object> res = new HashMap<>();
                    res.put("message", "This is a slow endpoint");
                    res.put("delaySeconds", 2);
                    res.put("timestamp", LocalDateTime.now());
                    return res;
                });
    }

    @GetMapping("/test/headers")
    public Mono<Map<String, Object>> testHeaders(@RequestHeader Map<String, String> headers) {
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Headers received");
        res.put("headers", headers);
        res.put("customHeadersAdded", "Check for X-Gateway-* headers");
        return Mono.just(res);
    }

}
