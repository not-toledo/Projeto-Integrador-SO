package com.pccheck.controller;

import com.pccheck.compatibility.GameCompatibilityEvaluator;
import com.pccheck.dto.GameCompatibilityDto;
import com.pccheck.dto.GameRequirementsDto;
import com.pccheck.dto.GameSearchResultDto;
import com.pccheck.service.InventoryService;
import com.pccheck.steam.SteamGameSearchService;
import com.pccheck.steam.SteamRequirementsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameCompatibilityController {
    private final SteamGameSearchService search;
    private final SteamRequirementsService requirements;
    private final InventoryService inventory;
    private final GameCompatibilityEvaluator evaluator;

    public GameCompatibilityController(SteamGameSearchService search, SteamRequirementsService requirements,
            InventoryService inventory, GameCompatibilityEvaluator evaluator) {
        this.search = search;
        this.requirements = requirements;
        this.inventory = inventory;
        this.evaluator = evaluator;
    }

    @GetMapping("/search")
    public List<GameSearchResultDto> search(@RequestParam String query) {
        return search.search(query);
    }

    @GetMapping("/{appId}/requirements")
    public GameRequirementsDto requirements(@PathVariable long appId,
            @RequestParam(defaultValue = "false") boolean refresh) {
        return requirements.get(appId, refresh);
    }

    @GetMapping("/{appId}/compatibility")
    public GameCompatibilityDto compatibility(@PathVariable long appId,
            @RequestParam(defaultValue = "false") boolean refresh) {
        GameRequirementsDto game = requirements.get(appId, refresh);
        return evaluator.evaluate(game, inventory.collect());
    }
}
