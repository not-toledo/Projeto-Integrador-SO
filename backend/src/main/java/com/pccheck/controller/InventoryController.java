package com.pccheck.controller;

import com.pccheck.dto.InventoryDto;
import com.pccheck.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint do Módulo A. Cada chamada faz uma nova coleta (atualização manual = nova requisição). */
@RestController
@RequestMapping("/api")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/inventory")
    public InventoryDto inventory() {
        return inventoryService.collect();
    }
}
