package com.pccheck.controller;

import com.pccheck.history.HistoryDto;
import com.pccheck.history.HistoryService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Histórico das coletas agendadas (gravado pelo agente; ver scripts/agendar-coleta). */
@RestController
@RequestMapping("/api")
public class HistoryController {

    private final HistoryService service;

    public HistoryController(HistoryService service) {
        this.service = service;
    }

    @GetMapping("/history")
    public HistoryDto history(@RequestParam(defaultValue = "7") int days) {
        return service.history(days);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
