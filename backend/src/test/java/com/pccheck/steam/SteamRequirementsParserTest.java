package com.pccheck.steam;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SteamRequirementsParserTest {
    private final SteamRequirementsParser parser = new SteamRequirementsParser();

    @Test
    void removesHtmlPreservesLinesAndParsesMemoryAndStorage() {
        String html = "<h3>Minimum:</h3><ul><li>Memory: 8 GB RAM</li><li>Storage: 100 GB available space</li></ul>";
        String plain = parser.toPlainText(html);
        assertTrue(plain.contains("Memory: 8 GB RAM"));
        assertTrue(plain.contains("Storage: 100 GB available space"));
        var fields = parser.parse(html);
        assertEquals(2, fields.size());
        assertEquals("RAM", fields.get(0).component());
        assertEquals(8_000_000_000L, fields.get(0).parsedBytes());
        assertEquals("STORAGE", fields.get(1).component());
        assertEquals(100_000_000_000L, fields.get(1).parsedBytes());
    }

    @Test
    void leavesUnknownCpuAndGpuForManualReview() {
        var fields = parser.parse("<li>Processor: Intel Core i5-8400</li><li>Graphics: NVIDIA GTX 1060</li>");
        assertEquals("MANUAL_REVIEW", fields.get(0).parseStatus());
        assertEquals("MANUAL_REVIEW", fields.get(1).parseStatus());
    }

    @Test
    void handlesMissingRequirements() {
        assertTrue(parser.parse(null).isEmpty());
        assertEquals("", parser.toPlainText(""));
    }
}
