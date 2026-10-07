package com.inventario.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Icono vectorial pensado para usarse directamente desde un fichero FXML:
 *
 * <pre>
 * &lt;?import com.inventario.ui.Icono?&gt;
 *
 * &lt;Button text="Nuevo producto"&gt;
 *     &lt;graphic&gt;&lt;Icono nombre="mas"/&gt;&lt;/graphic&gt;
 * &lt;/Button&gt;
 * </pre>
 *
 * Los trazados son de Material Design Icons (licencia Apache 2.0) dibujados
 * sobre una retícula de 24 x 24 y escalados al tamano pedido. El color no se
 * fija aqui: lo decide la hoja de estilos a traves de la clase {@code .icono},
 * lo que permite que el icono cambie con el estado del boton (hover, foco...).
 *
 * Nombres disponibles: panel, caja, lista, intercambio, etiqueta, camion,
 * grafica, grupo, datos, salir, entrar, persona, mas, menos, editar, borrar,
 * buscar, actualizar, codigo, documento, hoja, carpeta, alerta, dinero,
 * historial, check.
 */
public class Icono extends SVGPath {

    /** Lienzo de origen de todos los trazados. */
    private static final double LIENZO = 24.0;

    private static final Map<String, String> RUTAS = crearRutas();

    private final StringProperty nombre = new SimpleStringProperty(this, "nombre", "circulo");
    private final DoubleProperty tamano = new SimpleDoubleProperty(this, "tamano", 20);

    public Icono() {
        getStyleClass().add("icono");
        nombre.addListener((obs, anterior, nuevo) -> pintar());
        tamano.addListener((obs, anterior, nuevo) -> escalar());
        pintar();
        escalar();
    }

    public String getNombre() {
        return nombre.get();
    }

    public void setNombre(String valor) {
        nombre.set(valor == null ? "" : valor);
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public double getTamano() {
        return tamano.get();
    }

    public void setTamano(double valor) {
        tamano.set(valor);
    }

    public DoubleProperty tamanoProperty() {
        return tamano;
    }

    /** true si existe un trazado con ese nombre. */
    public static boolean existe(String nombreIcono) {
        return nombreIcono != null && RUTAS.containsKey(nombreIcono);
    }

    /** Todos los nombres de icono disponibles (para documentacion o pruebas). */
    public static Map<String, String> rutas() {
        return RUTAS;
    }

    private void pintar() {
        String ruta = RUTAS.get(getNombre());
        setContent(ruta == null ? RUTAS.get("circulo") : ruta);
    }

    /** Escala el trazado manteniéndolo centrado dentro de su caja de 24 x 24. */
    private void escalar() {
        double escala = Math.max(6, getTamano()) / LIENZO;
        getTransforms().setAll(new Scale(escala, escala, LIENZO / 2, LIENZO / 2));
    }

    private static Map<String, String> crearRutas() {
        Map<String, String> rutas = new LinkedHashMap<>();

        // ---- Navegacion y secciones ----
        rutas.put("panel", "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z");
        rutas.put("caja", "M20 2H4c-1.1 0-2 .9-2 2v3.01c0 .72.43 1.34 1 1.69V20c0 1.1 1.1 2 2 2h14c"
                + ".9 0 2-.9 2-2V8.7c.57-.35 1-.97 1-1.69V4c0-1.1-1-2-2-2zm-5 12H9v-2h6v2zm5-7H4V4h16v3z");
        rutas.put("lista", "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"
                + "m0 16H5V5h14v14zM7 10h2v2H7zm0 4h2v2H7zm4-4h6v2h-6zm0 4h6v2h-6z");
        rutas.put("intercambio", "M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z");
        rutas.put("etiqueta", "M17.63 5.84C17.27 5.33 16.67 5 16 5L5 5.01C3.9 5.01 3 5.9 3 7v10c0 1.1.9"
                + " 1.99 2 1.99L16 19c.67 0 1.27-.33 1.63-.84L22 12l-4.37-6.16z");
        rutas.put("camion", "M20 8h-3V4H3c-1.1 0-2 .9-2 2v11h2c0 1.66 1.34 3 3 3s3-1.34 3-3h6c0 1.66 1.34"
                + " 3 3 3s3-1.34 3-3h2v-5l-3-4zM6 18.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5"
                + " 1.5-.67 1.5-1.5 1.5zm13.5-9l1.96 2.5H17V9.5h2.5zm-1.5 9c-.83 0-1.5-.67-1.5-1.5s.67"
                + "-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z");
        rutas.put("grafica", "M5 9.2h3V19H5V9.2zM10.6 5h2.8v14h-2.8V5zm5.6 8H19v6h-2.8v-6z");
        rutas.put("grupo", "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3z"
                + "m-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7"
                + " 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97"
                + " 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z");
        rutas.put("datos", "M2 20h20v-4H2v4zm2-3h2v2H4v-2zM2 4v4h20V4H2zm4 3H4V5h2v2zm-4 7h20v-4H2v4zm2"
                + "-3h2v2H4v-2z");
        rutas.put("persona", "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8"
                + " 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z");

        // ---- Acciones ----
        rutas.put("mas", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z");
        rutas.put("menos", "M19 13H5v-2h14v2z");
        rutas.put("editar", "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02"
                + " 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
        rutas.put("borrar", "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        rutas.put("buscar", "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3"
                + " 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99"
                + "-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        rutas.put("actualizar", "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-8 8 0 4.42 3.58 8 8"
                + " 8 3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6"
                + "-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z");
        rutas.put("check", "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z");
        rutas.put("entrar", "M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 14h-8v2h8c1.1 0"
                + " 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z");
        rutas.put("salir", "M13 3h-2v10h2V3zm4.83 2.17l-1.42 1.42C17.99 7.86 19 9.81 19 12c0 3.87-3.13"
                + " 7-7 7s-7-3.13-7-7c0-2.19 1.01-4.14 2.58-5.42L6.17 5.17C4.23 6.82 3 9.26 3 12c0 4.97"
                + " 4.03 9 9 9s9-4.03 9-9c0-2.74-1.23-5.18-3.17-6.83z");

        // ---- Documentos e informes ----
        rutas.put("documento", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8"
                + "l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z");
        rutas.put("hoja", "M20 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h15c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0"
                + " 16H5V5h15v14zM7 7h2v2H7zm0 4h2v2H7zm4-4h2v2h-2zm0 4h2v2h-2zm4-4h2v2h-2zm0 4h2v2h-2z");
        rutas.put("carpeta", "M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9"
                + "-2-2-2h-8l-2-2z");
        rutas.put("codigo", "M2 5h2v14H2zm3.5 0h1v14h-1zM7.5 5h2.5v14H7.5zM11.5 5h1v14h-1zM14 5h1.5v14H14z"
                + "M17 5h2v14h-2zM20.5 5H22v14h-1.5z");

        // ---- Indicadores ----
        rutas.put("alerta", "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z");
        rutas.put("dinero", "M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5"
                + " 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91"
                + " 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h"
                + "-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81"
                + "-4.7-4.4z");
        rutas.put("historial", "M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7"
                + " 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c"
                + "4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z");

        // ---- Reserva ----
        rutas.put("circulo", "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 4a6 6 0 1 1 0 12 6 6 0 0 1 0-12z");

        return Collections.unmodifiableMap(rutas);
    }
}
