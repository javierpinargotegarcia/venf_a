package com.inventario.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/** Mensajes de dialogo reutilizables. */
public final class Alertas {

    private Alertas() {
    }

    public static void info(String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, "Informacion", null, mensaje);
    }

    public static void info(String cabecera, String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, "Informacion", cabecera, mensaje);
    }

    public static void error(String mensaje) {
        mostrar(Alert.AlertType.ERROR, "Error", null, mensaje);
    }

    public static void error(String cabecera, String mensaje) {
        mostrar(Alert.AlertType.ERROR, "Error", cabecera, mensaje);
    }

    public static void aviso(String mensaje) {
        mostrar(Alert.AlertType.WARNING, "Aviso", null, mensaje);
    }

    /** true si el usuario pulsa Aceptar. */
    public static boolean confirmar(String cabecera, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar");
        alerta.setHeaderText(cabecera);
        alerta.setContentText(mensaje);
        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == ButtonType.OK;
    }

    /** Dialogo con una sola casilla de texto (nombres, codigos, etc.). */
    public static Optional<String> pedirTexto(String cabecera, String mensaje, String inicial) {
        javafx.scene.control.TextInputDialog dialogo =
                new javafx.scene.control.TextInputDialog(inicial == null ? "" : inicial);
        dialogo.setTitle("Dato");
        dialogo.setHeaderText(cabecera);
        dialogo.setContentText(mensaje);
        return dialogo.showAndWait().map(String::trim).filter(s -> !s.isEmpty());
    }

    /** Muestra un error inesperado con su traza en un bloque desplegable. */
    public static void excepcion(String cabecera, Throwable e) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error");
        alerta.setHeaderText(cabecera);
        alerta.setContentText(e.getMessage());

        StringWriter escritor = new StringWriter();
        e.printStackTrace(new PrintWriter(escritor));
        TextArea area = new TextArea(escritor.toString());
        area.setEditable(false);
        area.setWrapText(true);
        area.setMaxWidth(Double.MAX_VALUE);
        area.setMaxHeight(Double.MAX_VALUE);
        GridPane.setHgrow(area, Priority.ALWAYS);
        GridPane.setVgrow(area, Priority.ALWAYS);

        GridPane contenido = new GridPane();
        contenido.setMaxWidth(Double.MAX_VALUE);
        contenido.add(new Label("Detalle tecnico:"), 0, 0);
        contenido.add(area, 0, 1);
        alerta.getDialogPane().setExpandableContent(contenido);
        alerta.showAndWait();
    }

    private static void mostrar(Alert.AlertType tipo, String titulo, String cabecera, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(cabecera);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
