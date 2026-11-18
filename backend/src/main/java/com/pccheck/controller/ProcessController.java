package com.pccheck.controller;

import com.pccheck.processes.ProcessService;
import com.pccheck.processes.ProcessesDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Processos em execução. Somente leitura: não há rota para encerrar ou alterar processos. */
@RestController
@RequestMapping("/api")
public class ProcessController {

    private final ProcessService service;

    public ProcessController(ProcessService service) {
        this.service = service;
    }

    @GetMapping("/processes")
    public ProcessesDto processes() {
        return service.processes();
    }
}
