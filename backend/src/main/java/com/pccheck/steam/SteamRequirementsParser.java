package com.pccheck.steam;

import com.pccheck.dto.RequirementFieldDto;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Interpreta os requisitos de PC publicados pela Steam (HTML) e sempre preserva o texto original.
 *
 * <p>A Steam publica cada requisito como uma linha com rótulo ("Processor:", "Memory:", "Graphics:"...).
 * O componente é identificado pelo rótulo; só linhas sem rótulo reconhecido usam palavras-chave como
 * alternativa. Assim, "Graphics: GTX 970, 4 GB memory" é GPU (com 4 GB de VRAM), e não RAM.
 */
@Component
public class SteamRequirementsParser {

    /** Quantidade com unidade de memória: "8 GB", "4096 MB", "6GB". */
    private static final Pattern MEMORY = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(gb|gib|mb|mib)\\b");
    private static final Pattern STORAGE = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(gb|gib|tb|tib|mb|mib)\\b");
    /** "Windows 10", "Windows® 7/8/10", "Windows 10/11 64-bit". */
    private static final Pattern WINDOWS = Pattern.compile("(?i)windows\\W*((?:\\d+(?:\\.\\d+)?)(?:\\s*(?:/|,|or|ou|e|and)\\s*\\d+(?:\\.\\d+)?)*)");
    private static final Pattern OTHER_OS = Pattern.compile("(?i)(mac\\s*os|macos|linux|steamos|ubuntu)");
    private static final Pattern CPU_HINT = Pattern.compile("(?i)(intel\\s+(?:core|pentium|celeron|xeon)|core\\s*i[3579]|\\bi[3579][- ]\\d{3,5}|amd\\s+(?:ryzen|athlon|fx)|ryzen|pentium|athlon|\\bfx-?\\d{4}|xeon)");
    private static final Pattern GPU_HINT = Pattern.compile("(?i)(nvidia|geforce|\\bgtx\\s*\\d+|\\brtx\\s*\\d+|radeon|\\brx\\s*\\d+|intel\\s*(?:hd|uhd|iris|arc))");
    private static final Pattern SECTION_HEADER = Pattern.compile(
            "(?i)^(minimum|recommended|minimos?|recomendados?|requisitos\\s+(?:minimos|recomendados))\\s*:?$");
    /** "Rótulo: valor", com rótulo curto (até 40 caracteres, sem dois-pontos). */
    private static final Pattern LABELED = Pattern.compile("^([^:]{1,40}):\\s*(.*)$");

    /** Rótulos da Steam (em inglês e português, sem acentos e em minúsculas) → componente. */
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("os", "OS"), Map.entry("so", "OS"), Map.entry("operating system", "OS"),
            Map.entry("sistema operacional", "OS"), Map.entry("sistema operativo", "OS"),
            Map.entry("processor", "CPU"), Map.entry("processador", "CPU"), Map.entry("cpu", "CPU"),
            Map.entry("memory", "RAM"), Map.entry("memoria", "RAM"), Map.entry("ram", "RAM"),
            Map.entry("graphics", "GPU"), Map.entry("graphics card", "GPU"), Map.entry("video card", "GPU"),
            Map.entry("placa de video", "GPU"), Map.entry("video", "GPU"), Map.entry("gpu", "GPU"),
            Map.entry("storage", "STORAGE"), Map.entry("hard drive", "STORAGE"), Map.entry("hard disk", "STORAGE"),
            Map.entry("hard disk space", "STORAGE"), Map.entry("armazenamento", "STORAGE"),
            Map.entry("disco rigido", "STORAGE"), Map.entry("espaco em disco", "STORAGE"));

    public String toPlainText(String html) {
        if (html == null || html.isBlank()) return "";
        String text = html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</li\\s*>", "\n")
                .replaceAll("(?i)</p\\s*>", "\n")
                .replaceAll("(?i)</h[1-6]\\s*>", "\n")
                .replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&#160;", " ")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&apos;", "'").replace("&#39;", "'")
                .replace("&eacute;", "é").replace("&Eacute;", "É")
                .replace("&aacute;", "á").replace("&Aacute;", "Á")
                .replace("&atilde;", "ã").replace("&Atilde;", "Ã")
                .replace("&ccedil;", "ç").replace("&Ccedil;", "Ç")
                .replace("&reg;", "®").replace("&trade;", "™").replace("&copy;", "©");
        Matcher entities = Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(text);
        StringBuilder decoded = new StringBuilder();
        while (entities.find()) {
            try {
                String code = entities.group(1);
                int value = code.startsWith("x") || code.startsWith("X")
                        ? Integer.parseInt(code.substring(1), 16) : Integer.parseInt(code);
                entities.appendReplacement(decoded, Matcher.quoteReplacement(new String(Character.toChars(value))));
            } catch (RuntimeException invalidEntity) {
                entities.appendReplacement(decoded, Matcher.quoteReplacement(entities.group()));
            }
        }
        entities.appendTail(decoded);
        // Quebras de linha separam os requisitos. A substituição usa Matcher.quoteReplacement:
        // "\\n" em uma substituição de replaceAll vira a letra "n" e juntava todas as linhas em uma só.
        String newline = Matcher.quoteReplacement("\n");
        return decoded.toString().replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n *", newline).replaceAll("\\n{2,}", newline).trim();
    }

    public List<RequirementFieldDto> parse(String html) {
        return parseText(toPlainText(html));
    }

    /** Interpreta texto já limpo por {@link #toPlainText(String)}, uma linha por requisito. */
    public List<RequirementFieldDto> parseText(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<RequirementFieldDto> result = new ArrayList<>();
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || SECTION_HEADER.matcher(simplify(line)).matches()) continue;
            Matcher labeled = LABELED.matcher(line);
            if (labeled.matches() && SECTION_HEADER.matcher(simplify(labeled.group(1))).matches()) {
                // Jogos antigos: "Mínimo: Processador de 1.7 GHz, 512 MB de RAM, ..." em um único parágrafo.
                for (String item : splitTopLevel(labeled.group(2))) {
                    result.add(field(byKeyword(item), item, item));
                }
                continue;
            }
            String component;
            String value = line;
            if (labeled.matches()) {
                // Rótulo sem comparação (DirectX, Rede, Placa de som, Notas adicionais...) vira OTHER.
                component = labelComponent(labeled.group(1));
                value = labeled.group(2);
            } else {
                component = byKeyword(line);
            }
            result.add(field(component, line, value));
        }
        return List.copyOf(result);
    }

    /**
     * Posição do bloco "Recomendado:"/"Recommended:" publicado dentro do texto dos mínimos (comum em jogos
     * antigos), ou -1. O texto deve vir de {@link #toPlainText(String)}.
     */
    public int embeddedRecommendedStart(String text) {
        if (text == null) return -1;
        int offset = 0;
        for (String line : text.split("\n", -1)) {
            Matcher labeled = LABELED.matcher(line.trim());
            String head = labeled.matches() ? labeled.group(1) : line;
            if (offset > 0 && simplify(head).matches("recomendados?|recommended|requisitos recomendados")) {
                return offset;
            }
            offset += line.length() + 1;
        }
        return -1;
    }

    /** Divide por vírgula/ponto e vírgula fora de parênteses: "Pentium 4 (3.0 GHz, ou melhor)" fica inteiro. */
    static List<String> splitTopLevel(String text) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (char c : text.toCharArray()) {
            if (c == '(') depth++;
            if (c == ')' && depth > 0) depth--;
            if ((c == ',' || c == ';') && depth == 0) {
                if (!current.toString().isBlank()) items.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.toString().isBlank()) items.add(current.toString().trim());
        return items;
    }

    /** "Graphics:", "Graphics (AMD):" e "Placa de vídeo:" são GPU; rótulos não comparáveis viram OTHER. */
    private static String labelComponent(String label) {
        String simple = simplify(label);
        String exact = LABELS.get(simple);
        if (exact != null) return exact;
        for (Map.Entry<String, String> entry : LABELS.entrySet()) {
            if (simple.startsWith(entry.getKey() + " ") || simple.startsWith(entry.getKey() + "(")) {
                return entry.getValue();
            }
        }
        return "OTHER";
    }

    /** Alternativa para linhas sem rótulo: a ordem evita confundir "4 GB de memória" da GPU com RAM. */
    private static String byKeyword(String line) {
        String lower = simplify(line);
        // Aviso comum da Steam; não é um requisito de processador.
        if (lower.startsWith("requires a 64-bit") || lower.startsWith("requer um processador")) return "OTHER";
        if (WINDOWS.matcher(line).find() || OTHER_OS.matcher(line).find()) return "OS";
        if (CPU_HINT.matcher(line).find() || lower.contains("processor") || lower.contains("processador")) return "CPU";
        if (GPU_HINT.matcher(line).find() || lower.contains("graphics") || lower.contains("vram")
                || lower.contains("placa de video") || lower.contains("video card")) return "GPU";
        if (lower.contains("memory") || lower.matches(".*\\bram\\b.*") || lower.contains("memoria")) return "RAM";
        if (lower.contains("storage") || lower.contains("available space") || lower.contains("armazenamento")) return "STORAGE";
        return "OTHER";
    }

    private static RequirementFieldDto field(String component, String line, String value) {
        return switch (component) {
            case "RAM" -> numeric("RAM", line, value, MEMORY);
            case "STORAGE" -> numeric("STORAGE", line, value, STORAGE);
            case "OS" -> os(line, value);
            case "CPU" -> new RequirementFieldDto("CPU", line, null, null, 0.35, "MANUAL_REVIEW");
            case "GPU" -> {
                Long vram = numeric("GPU", line, value, MEMORY).parsedBytes();
                yield new RequirementFieldDto("GPU", line, vram, null, vram == null ? 0.35 : 0.75, "MANUAL_REVIEW");
            }
            default -> new RequirementFieldDto("OTHER", line, null, null, 0.0, "UNPARSED");
        };
    }

    /** Sistema operacional: guarda a menor versão do Windows citada ("Windows 7/8/10" → "Windows 7"). */
    private static RequirementFieldDto os(String line, String value) {
        Matcher m = WINDOWS.matcher(value);
        if (m.find()) {
            Double lowest = null;
            for (String part : m.group(1).split("[^0-9.]+")) {
                if (part.isBlank()) continue;
                try {
                    double v = Double.parseDouble(part);
                    if (lowest == null || v < lowest) lowest = v;
                } catch (NumberFormatException ignored) {
                    // parte não numérica
                }
            }
            if (lowest != null) {
                String version = lowest == Math.floor(lowest) ? String.valueOf(lowest.intValue()) : lowest.toString();
                return new RequirementFieldDto("OS", line, null, "Windows " + version, 0.9, "PARSED");
            }
        }
        Matcher other = OTHER_OS.matcher(value);
        String parsed = other.find() ? other.group(1) : null;
        return new RequirementFieldDto("OS", line, null, parsed, parsed == null ? 0.3 : 0.6,
                parsed == null ? "MANUAL_REVIEW" : "PARSED");
    }

    private static RequirementFieldDto numeric(String component, String line, String value, Pattern pattern) {
        Matcher m = pattern.matcher(value);
        if (!m.find()) return new RequirementFieldDto(component, line, null, null, 0.0, "UNPARSED");
        try {
            double amount = Double.parseDouble(m.group(1).replace(',', '.'));
            String unit = m.group(2).toLowerCase(Locale.ROOT);
            int power = unit.startsWith("t") ? 4 : unit.startsWith("g") ? 3 : 2;
            double base = unit.endsWith("ib") ? 1024.0 : 1000.0;
            long bytes = (long) (amount * Math.pow(base, power));
            String amountText = amount == Math.floor(amount) ? String.valueOf((long) amount) : String.valueOf(amount);
            return new RequirementFieldDto(component, line, bytes, amountText + " " + unit.toUpperCase(Locale.ROOT), 0.95, "PARSED");
        } catch (RuntimeException e) {
            return new RequirementFieldDto(component, line, null, null, 0.0, "UNPARSED");
        }
    }

    /** Minúsculas, sem acentos, símbolos (® ™) nem espaços repetidos. */
    static String simplify(String text) {
        String plain = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[®™©]", "").replaceAll("\\s+", " ").trim();
    }
}
