package com.pccheck.controller;

import com.pccheck.diagnostic.HealthReport;
import com.pccheck.service.HealthCheckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint do Módulo B. Cada chamada faz uma nova coleta e avaliação; não altera nada no sistema. */
@RestController
@RequestMapping("/api")
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    public HealthCheckController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping("/health-check")
    public HealthReport healthCheck() {
        return healthCheckService.check();
    }
}
