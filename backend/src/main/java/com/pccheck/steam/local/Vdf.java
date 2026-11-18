package com.pccheck.steam.local;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Leitor do formato de texto KeyValues da Steam (VDF/ACF), usado por {@code libraryfolders.vdf} e
 * {@code appmanifest_*.acf}: {@code "chave" "valor"} ou {@code "chave" { ... }}, com comentários {@code //}.
 *
 * <p>As chaves são guardadas em minúsculas: a Steam não diferencia maiúsculas nelas ({@code "AppState"} e
 * {@code "appstate"} aparecem em versões diferentes). Escapes aceitos: {@code \\ \" \n \t}.
 */
public final class Vdf {

    /** Limite de aninhamento: os arquivos reais têm 4 níveis; mais que isso indica arquivo inválido. */
    private static final int MAX_DEPTH = 32;

    private final String text;
    private int pos;

    private Vdf(String text) {
        this.text = text;
    }

    /** Lê o documento inteiro como um bloco. Lança {@link IllegalArgumentException} se o texto for inválido. */
    public static Map<String, Object> parse(String text) {
        Vdf vdf = new Vdf(text);
        Map<String, Object> root = vdf.block(0, false);
        vdf.skipBlank();
        if (vdf.pos < text.length()) {
            throw vdf.error("conteúdo após o fim do documento");
        }
        return root;
    }

    /** Sub-bloco pela chave (sem diferenciar maiúsculas); null se não existir ou não for bloco. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> block(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key.toLowerCase(Locale.ROOT));
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    /** Valor de texto pela chave (sem diferenciar maiúsculas); null se não existir ou for bloco. */
    public static String string(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key.toLowerCase(Locale.ROOT));
        return value instanceof String s ? s : null;
    }

    private Map<String, Object> block(int depth, boolean closedByBrace) {
        if (depth > MAX_DEPTH) {
            throw error("aninhamento excessivo");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        while (true) {
            skipBlank();
            if (pos >= text.length()) {
                if (closedByBrace) {
                    throw error("bloco sem '}'");
                }
                return map;
            }
            char c = text.charAt(pos);
            if (c == '}') {
                if (!closedByBrace) {
                    throw error("'}' sem bloco aberto");
                }
                pos++;
                return map;
            }
            String key = token().toLowerCase(Locale.ROOT);
            skipBlank();
            if (pos < text.length() && text.charAt(pos) == '{') {
                pos++;
                map.put(key, block(depth + 1, true));
            } else {
                map.put(key, token());
            }
        }
    }

    /** Texto entre aspas ou palavra sem aspas (a Steam aceita os dois). */
    private String token() {
        if (pos >= text.length()) {
            throw error("valor esperado");
        }
        StringBuilder sb = new StringBuilder();
        if (text.charAt(pos) == '"') {
            pos++;
            while (pos < text.length()) {
                char c = text.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\' && pos < text.length()) {
                    char next = text.charAt(pos++);
                    sb.append(switch (next) {
                        case 'n' -> '\n';
                        case 't' -> '\t';
                        default -> next; // \\ e \"
                    });
                } else {
                    sb.append(c);
                }
            }
            throw error("texto sem aspas de fechamento");
        }
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (Character.isWhitespace(c) || c == '{' || c == '}' || c == '"') {
                break;
            }
            sb.append(c);
            pos++;
        }
        if (sb.isEmpty()) {
            throw error("valor esperado");
        }
        return sb.toString();
    }

    private void skipBlank() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (Character.isWhitespace(c) || c == '﻿') {
                pos++;
            } else if (c == '/' && pos + 1 < text.length() && text.charAt(pos + 1) == '/') {
                while (pos < text.length() && text.charAt(pos) != '\n') {
                    pos++;
                }
            } else {
                return;
            }
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("VDF inválido (posição " + pos + "): " + message);
    }
}
