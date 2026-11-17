package com.pccheck.controller;

import com.pccheck.dto.LiveDto;
import com.pccheck.dto.LiveHistoryDto;
import com.pccheck.service.LiveMonitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints do Módulo C. Apenas leem o histórico em memória; a coleta ocorre em segundo plano. */
@RestController
@RequestMapping("/api/live")
public class LiveController {

    private final LiveMonitorService service;

    public LiveController(LiveMonitorService service) {
        this.service = service;
    }

    /** Amostra mais recente e estado da coleta. */
    @GetMapping
    public LiveDto live() {
        return service.live();
    }

    /** Últimas amostras (padrão: todas as mantidas em memória). */
    @GetMapping("/history")
    public LiveHistoryDto history(@RequestParam(name = "limit", defaultValue = "3600") int limit) {
        return service.history(limit);
    }
}
