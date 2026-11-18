package com.pccheck.diagnostic;

import java.util.Locale;

/** Formatação de valores com unidade explícita. */
public final class Formatting {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private Formatting() {
    }

    public static String percent(double value) {
        return String.format(PT_BR, "%.1f%%", value);
    }

    public static String celsius(double value) {
        return String.format(PT_BR, "%.1f °C", value);
    }

    /** Bytes em unidade binária (KiB, MiB, GiB, TiB). */
    public static String bytes(long bytes) {
        String[] units = {"B", "KiB", "MiB", "GiB", "TiB"};
        double value = bytes;
        int i = 0;
        while (value >= 1024.0 && i < units.length - 1) {
            value /= 1024.0;
            i++;
        }
        return i == 0 ? bytes + " B" : String.format(PT_BR, "%.1f %s", value, units[i]);
    }
}
