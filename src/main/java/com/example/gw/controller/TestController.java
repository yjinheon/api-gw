package com.example.gw.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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



        return Mono.just(res);

    }

    @GetMapping("/info")
    public Mono<Map<String, Object>> info() {
        Map<String, Object> res = new HashMap<>();
        res.put("gateway","Spring Cloud Gateway");
        res.put("routes","3 routes configured");
        res.put("filters","basic filters enabled");
        return Mono.just(res);

    }
}
