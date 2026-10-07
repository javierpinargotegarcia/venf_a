package com.inventario.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Formatos de fecha/hora usados en toda la aplicacion. */
public final class UtilFechas {

    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private UtilFechas() {
    }

    public static String formatear(LocalDateTime fecha) {
        return fecha == null ? "" : FECHA_HORA.format(fecha);
    }

    public static String formatearFecha(LocalDateTime fecha) {
        return fecha == null ? "" : FECHA.format(fecha);
    }

    /** Nombre de archivo con sello de tiempo: {@code Inventario_20260101_103000.pdf} */
    public static String sello(String prefijo, String extension) {
        return prefijo + "_" + LocalDateTime.now().format(ARCHIVO) + "." + extension;
    }
}
