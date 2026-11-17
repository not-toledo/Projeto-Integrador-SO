package com.pccheck.steam;

import com.pccheck.dto.RequirementFieldDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Parser tolerante a HTML e texto livre; sempre preserva o conteúdo original. */
@Component
public class SteamRequirementsParser {
    private static final Pattern MEMORY = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(gb|gib|mb|mib)\\s*(?:ram|memory|mem[oó]ria)?");
    private static final Pattern STORAGE = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(gb|gib|tb|tib)\\s*(?:available\\s+space|free\\s+space|storage|armazenamento|livre)?");
    private static final Pattern OS = Pattern.compile("(?i)(windows\\s*(?:7|8|10|11)|windows\\s*\\d+|macos|mac\\s*os|linux)");
    private static final Pattern CPU = Pattern.compile("(?i)(intel\\s+(?:core|pentium|celeron|xeon)|core\\s*i[3579]|amd\\s+(?:ryzen|athlon)|ryzen|pentium|athlon|fx-?\\d+|xeon).{0,100}");
    private static final Pattern GPU = Pattern.compile("(?i)(nvidia|geforce|gtx\\s*\\d+|rtx\\s*\\d+|radeon|rx\\s*\\d+|intel\\s*(?:hd|uhd|iris)).{0,100}");

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
                .replace("&ccedil;", "ç").replace("&Ccedil;", "Ç");
        Matcher entities = Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(text);
        StringBuffer decoded = new StringBuffer();
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
        return decoded.toString().replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n *", "\\n").replaceAll("\\n{2,}", "\\n").trim();
    }

    public List<RequirementFieldDto> parse(String html) {
        String text = toPlainText(html);
        if (text.isBlank()) return List.of();
        List<RequirementFieldDto> result = new ArrayList<>();
        for (String raw : text.split("\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.matches("(?i)minimum|recommended|requisitos m[ií]nimos|recomendados")) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("memory") || lower.matches("(?s).*\\bram\\b.*") || lower.contains("memória")) {
                result.add(numeric("RAM", line, MEMORY));
            } else if (lower.contains("storage") || lower.contains("available space") || lower.contains("armazenamento") || lower.contains("hard drive")) {
                result.add(numeric("STORAGE", line, STORAGE));
            } else if (OS.matcher(line).find()) {
                result.add(new RequirementFieldDto("OS", line, null, OS.matcher(line).results().findFirst().map(m -> m.group()).orElse(null), 0.85, "PARSED"));
            } else if (CPU.matcher(line).find() || lower.contains("processor")) {
                result.add(new RequirementFieldDto("CPU", line, null, null, 0.35, "MANUAL_REVIEW"));
            } else if (GPU.matcher(line).find() || lower.contains("graphics") || lower.contains("video card") || lower.contains("vram")) {
                RequirementFieldDto vram = numeric("GPU", line, MEMORY);
                result.add(new RequirementFieldDto("GPU", line, vram.parsedBytes(), null,
                        vram.parsedBytes() == null ? 0.35 : 0.75, "MANUAL_REVIEW"));
            } else {
                result.add(new RequirementFieldDto("OTHER", line, null, null, 0.0, "UNPARSED"));
            }
        }
        return List.copyOf(result);
    }

    private RequirementFieldDto numeric(String component, String line, Pattern pattern) {
        Matcher m = pattern.matcher(line);
        if (!m.find()) return new RequirementFieldDto(component, line, null, null, 0.0, "UNPARSED");
        try {
            double amount = Double.parseDouble(m.group(1).replace(',', '.'));
            String unit = m.group(2).toLowerCase(Locale.ROOT);
            int power = unit.startsWith("t") ? 4 : unit.startsWith("g") ? 3 : 2;
            double base = unit.endsWith("ib") ? 1024.0 : 1000.0;
            long bytes = (long) (amount * Math.pow(base, power));
            return new RequirementFieldDto(component, line, bytes, amount + " " + unit.toUpperCase(Locale.ROOT), 0.95, "PARSED");
        } catch (RuntimeException e) {
            return new RequirementFieldDto(component, line, null, null, 0.0, "UNPARSED");
        }
    }
}
