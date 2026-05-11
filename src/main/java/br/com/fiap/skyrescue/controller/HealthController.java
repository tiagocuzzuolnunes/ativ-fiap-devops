package br.com.fiap.skyrescue.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Value("${app.environment:local}")
    private String environment;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "application", "skyrescue-api",
                "status", "UP",
                "environment", environment,
                "timestamp", LocalDateTime.now()
        );
    }
}
