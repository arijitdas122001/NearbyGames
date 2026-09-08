package com.gameconnect.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        System.out.println("I am in the controller");
        return Map.of(
                "status", "UP",
                "service", "game-connect",
                "timestamp", Instant.now()
        );
    }
}
