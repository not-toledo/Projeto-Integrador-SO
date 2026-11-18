package com.pccheck.steam;

import static org.junit.jupiter.api.Assertions.*;

import com.pccheck.dto.RequirementFieldDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class SteamRequirementsParserTest {
    private final SteamRequirementsParser parser = new SteamRequirementsParser();

    /** Formato real da Steam (pc_requirements.minimum). */
    private static final String STEAM_HTML = "<strong>Minimum:</strong><br><ul class=\"bb_ul\">"
            + "<li>Requires a 64-bit processor and operating system<br></li>"
            + "<li><strong>OS:</strong> Windows® 7/8/10 64-bit<br></li>"
            + "<li><strong>Processor:</strong> Intel Core i5-8400 / AMD Ryzen 5 3600<br></li>"
            + "<li><strong>Memory:</strong> 8 GB RAM<br></li>"
            + "<li><strong>Graphics:</strong> NVIDIA GeForce GTX 970, 4 GB memory<br></li>"
            + "<li><strong>DirectX:</strong> Version 12<br></li>"
            + "<li><strong>Storage:</strong> 70 GB available space<br></li>"
            + "<li><strong>Additional Notes:</strong> SSD recommended</li></ul>";

    private static RequirementFieldDto only(List<RequirementFieldDto> fields, String component) {
        List<RequirementFieldDto> found = fields.stream().filter(f -> f.component().equals(component)).toList();
        assertEquals(1, found.size(), "esperava exatamente um campo " + component + " em " + fields);
        return found.get(0);
    }

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
    void paragrafoComCabecalhoEVirgulasViraUmCampoPorItem() {
        var fields = parser.parse("<p><strong>Minimum: </strong>1.7 GHz Processor, 512MB RAM, "
                + "DirectX&reg; 8.1 level Graphics Card, Windows&reg; 7 (32/64-bit), Mouse</p>");
        assertEquals("CPU", fields.get(0).component());
        assertEquals("RAM", fields.get(1).component());
        assertEquals(512_000_000L, fields.get(1).parsedBytes());
        assertEquals("GPU", fields.get(2).component());
        assertEquals("OS", fields.get(3).component());
        assertEquals("Windows 7", fields.get(3).parsedValue());
        assertEquals("OTHER", fields.get(4).component());
    }

    @Test
    void entidadesComunsDaSteamSaoDecodificadas() {
        assertEquals("Windows® 10 e Intel™", parser.toPlainText("Windows&reg; 10 e Intel&trade;"));
    }

    @Test
    void virgulaDentroDeParentesesNaoSeparaItens() {
        assertEquals(java.util.List.of("Pentium 4 (3.0 GHz, ou melhor)", "1 GB de RAM"),
                SteamRequirementsParser.splitTopLevel("Pentium 4 (3.0 GHz, ou melhor), 1 GB de RAM"));
    }

    @Test
    void localizaBlocoRecomendadoDentroDosMinimos() {
        String text = "Mínimo: 512 MB de RAM\nRecomendado: 1 GB de RAM";
        assertEquals(text.indexOf("Recomendado"), parser.embeddedRecommendedStart(text));
        assertEquals(-1, parser.embeddedRecommendedStart("Mínimo: 512 MB de RAM"));
        // Cabeçalho na primeira linha não é um bloco embutido.
        assertEquals(-1, parser.embeddedRecommendedStart("Recommended:\nMemory: 16 GB RAM"));
    }

    @Test
    void quebrasDeLinhaSeparamOsRequisitos() {
        String plain = parser.toPlainText("<li>Memory: 8 GB RAM</li><li>Storage: 20 GB</li>");
        assertEquals("Memory: 8 GB RAM\nStorage: 20 GB", plain);
    }

    @Test
    void leavesUnknownCpuAndGpuForManualReview() {
        var fields = parser.parse("<li>Processor: Intel Core i5-8400</li><li>Graphics: NVIDIA GTX 1060</li>");
        assertEquals(2, fields.size());
        assertEquals("MANUAL_REVIEW", fields.get(0).parseStatus());
        assertEquals("MANUAL_REVIEW", fields.get(1).parseStatus());
    }

    @Test
    void htmlRealDaSteamGeraUmCampoPorRequisito() {
        var fields = parser.parse(STEAM_HTML);
        assertEquals("Windows 7", only(fields, "OS").parsedValue());
        assertEquals("CPU", only(fields, "CPU").component());
        assertEquals(8_000_000_000L, only(fields, "RAM").parsedBytes());
        assertEquals(70_000_000_000L, only(fields, "STORAGE").parsedBytes());
        // DirectX, notas e o aviso de 64 bits não são comparáveis.
        assertEquals(3, fields.stream().filter(f -> f.component().equals("OTHER")).count());
    }

    @Test
    void memoriaDaPlacaDeVideoNaoViraRequisitoDeRam() {
        RequirementFieldDto gpu = only(parser.parse(STEAM_HTML), "GPU");
        assertEquals(4_000_000_000L, gpu.parsedBytes());
        assertTrue(gpu.rawText().contains("GTX 970"));
    }

    @Test
    void rotulosEmPortuguesERotulosComComplemento() {
        var fields = parser.parse("<li>Sistema operacional: Windows 10</li><li>Memória: 16 GB de RAM</li>"
                + "<li>Placa de vídeo: GeForce RTX 3060</li><li>Graphics (AMD): Radeon RX 6600</li>"
                + "<li>Armazenamento: 1 TB disponível</li>");
        assertEquals("Windows 10", only(fields, "OS").parsedValue());
        assertEquals(16_000_000_000L, only(fields, "RAM").parsedBytes());
        assertEquals(2, fields.stream().filter(f -> f.component().equals("GPU")).count());
        assertEquals(1_000_000_000_000L, only(fields, "STORAGE").parsedBytes());
    }

    @Test
    void linhasSemRotuloUsamPalavrasChave() {
        var fields = parser.parse("<li>Windows 10 64-bit</li><li>Intel Core i7-4790</li><li>GeForce GTX 1070</li>"
                + "<li>8 GB RAM</li>");
        assertEquals(List.of("OS", "CPU", "GPU", "RAM"), fields.stream().map(RequirementFieldDto::component).toList());
    }

    @Test
    void cabecalhoDeSecaoEIgnorado() {
        assertTrue(parser.parse("<strong>Recommended:</strong><br>").isEmpty());
        assertTrue(parser.parse("<h3>Requisitos mínimos</h3>").isEmpty());
    }

    @Test
    void sistemaSemWindowsNaoViraVersaoDoWindows() {
        RequirementFieldDto os = only(parser.parse("<li>OS: Ubuntu 22.04</li>"), "OS");
        assertEquals("Ubuntu", os.parsedValue());
    }

    @Test
    void handlesMissingRequirements() {
        assertTrue(parser.parse(null).isEmpty());
        assertEquals("", parser.toPlainText(""));
    }

    @Test
    void ssdExigidoViraRequisitoERecomendadoNao() {
        assertTrue(SteamRequirementsParser.requiresSsd("Outras observações: SSD required."));
        assertTrue(SteamRequirementsParser.requiresSsd("Storage: 70 GB available space (SSD)"));
        assertTrue(SteamRequirementsParser.requiresSsd("Armazenamento: requer SSD"));
        assertFalse(SteamRequirementsParser.requiresSsd("Storage: 50 GB (SSD recommended)"));
        assertFalse(SteamRequirementsParser.requiresSsd("SSD preferível"));
        assertFalse(SteamRequirementsParser.requiresSsd("Storage: 20 GB HDD/SSD"));
        assertFalse(SteamRequirementsParser.requiresSsd("Graphics: SSDX 9000"));
    }

    @Test
    void observacaoDoCyberpunkGeraRequisitoDeSsd() {
        var fields = parser.parse("<li><strong>Armazenamento:</strong> 70 GB de espaço disponível</li>"
                + "<li><strong>Outras observações:</strong> SSD required.</li>");
        assertEquals(java.util.List.of("STORAGE", "OTHER", "SSD"), fields.stream().map(f -> f.component()).toList());
        assertEquals("SSD", fields.get(2).parsedValue());
    }
}
