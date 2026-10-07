package com.inventario.util;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

/** Utilidades varias de presentacion. */
public final class UtilTextos {

    private UtilTextos() {
    }

    /** Formatea un importe: 1234.5 -> "1.234,50" */
    public static String dinero(BigDecimal valor) {
        BigDecimal v = valor == null ? BigDecimal.ZERO : valor;
        return String.format(Locale.forLanguageTag("es-ES"), "%,.2f", v);
    }

    /** Formatea un importe con simbolo de moneda. */
    public static String moneda(BigDecimal valor) {
        return dinero(valor) + " €";
    }

    /** Normaliza texto para busquedas sin acentos ni mayusculas. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return limpio.replaceAll("\\p{M}", "").trim();
    }

    /** true si {@code texto} contiene {@code busqueda} (sin distinguir mayusculas/acentos). */
    public static boolean contiene(String texto, String busqueda) {
        return normalizar(texto).contains(normalizar(busqueda));
    }

    /** Corta el texto a la longitud indicada añadiendo "..." */
    public static String recortar(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 3) + "...";
    }
}
