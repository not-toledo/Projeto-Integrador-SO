package com.pccheck.compatibility;

import static org.junit.jupiter.api.Assertions.*;
import com.pccheck.dto.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameCompatibilityEvaluatorTest {
    private final GameCompatibilityEvaluator evaluator = new GameCompatibilityEvaluator();

    private InventoryDto pc(long ram, long free) {
        return new InventoryDto(Instant.now(), "test",
                new ComponentDto<>("OK", null, null),
                new ComponentDto<>("OK", null, null),
                new ComponentDto<>("OK", null, new com.pccheck.agent.model.MemoryInfo(ram, 0L, ram, 100.0)),
                new ComponentDto<>("UNAVAILABLE", "n/a", null),
                new ComponentDto<>("UNAVAILABLE", "n/a", null),
                new ComponentDto<>("OK", null, new com.pccheck.agent.model.StorageInfo(null,
                        List.of(new com.pccheck.agent.model.VolumeInfo("C:", null, null, free, free, 0L, 0.0)))));
    }

    @Test
    void reportsBelowMinimumWhenRamIsInsufficient() {
        var ram = new RequirementFieldDto("RAM", "8 GB RAM", 8L * 1024 * 1024 * 1024, "8 GB", .95, "PARSED");
        var game = new GameRequirementsDto(1, "Game", null, "url", "8 GB RAM", "", List.of(ram), List.of(), Instant.now(), "Steam", List.of());
        var result = evaluator.evaluate(game, pc(4L * 1024 * 1024 * 1024, 20L * 1024 * 1024 * 1024));
        assertEquals("Abaixo dos mínimos", result.overallMinimum());
        assertEquals("BELOW_MINIMUM", result.minimumResults().get(0).status());
    }

    @Test
    void neverTreatsUnknownAsCompatible() {
        var cpu = new RequirementFieldDto("CPU", "Intel Core i5", null, null, .3, "MANUAL_REVIEW");
        var game = new GameRequirementsDto(1, "Game", null, "url", "Intel Core i5", "", List.of(cpu), List.of(), Instant.now(), "Steam", List.of());
        var result = evaluator.evaluate(game, pc(16L * 1024 * 1024 * 1024, 20L * 1024 * 1024 * 1024));
        assertEquals("Análise incompleta", result.overallMinimum());
        assertEquals("MANUAL_REVIEW", result.minimumResults().get(0).status());
    }
}
