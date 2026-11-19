package com.pccheck.controller;

import com.pccheck.dto.HealthDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint mínimo da V0.1 para confirmar que a API está no ar. Não é o Health Check do módulo B. */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/ping")
    public HealthDto ping() {
        return new HealthDto("UP", "NEXCORE", "4.0.0");
    }
}
