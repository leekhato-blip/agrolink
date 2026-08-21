package com.agrolink.controller;

import com.agrolink.model.HealthResponse;
import com.agrolink.service.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final GraphService graphService;

    public HealthController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(graphService.health());
    }
}
