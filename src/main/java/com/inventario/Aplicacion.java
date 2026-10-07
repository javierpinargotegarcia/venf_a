package com.inventario;

import com.inventario.config.ConfigApp;
import com.inventario.datos.DatosSemilla;
import com.inventario.seguridad.Sesion;
import com.inventario.ui.ControladorPrincipal;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.nio.file.Path;

/**
 * Punto de entrada de la aplicacion de escritorio (JavaFX).
 *
 * <p>Arranca en modo <b>JSON</b> (repositorio de ficheros por carpetas) y,
 * cuando el usuario lo indique, migra a <b>MySQL</b> sin perder datos.
 */
public class Aplicacion extends Application {

    private static final String CSS = "/styles.css";

    private static Stage escenario;
    private static StackPane panelContenido;
    private static Label etiquetaEstado;

    @Override
    public void start(Stage stage) {
        escenario = stage;
        escenario.setTitle("Sistema de Inventario de Mercaderia");
        escenario.setMinWidth(1024);
        escenario.setMinHeight(680);

        ConfigApp config = ConfigApp.cargar();
        AppContexto.inicializar(config);

        if (config.esModoMysql()) {
            arrancarModoMysql(config);
        } else {
            DatosSemilla.sembrarSiVacio();
        }

        mostrarLogin();
    }

    /** Si MySQL no responde, se vuelve a modo JSON en lugar de bloquear el arranque. */
    private void arrancarModoMysql(ConfigApp config) {
        try {
            com.inventario.db.ConexionMySQL.obtener(config).inicializarEsquema();
        } catch (Exception e) {
            System.err.println("[inventario] MySQL no disponible, se arranca en modo JSON: "
                    + e.getMessage());
            config.setModo(ConfigApp.MODO_JSON);
            config.guardar();
            com.inventario.repositorio.FactoriaRepositorios.inicializar(config);
            DatosSemilla.sembrarSiVacio();

            Alert aviso = new Alert(Alert.AlertType.WARNING,
                    "No se pudo conectar con MySQL, asi que la aplicacion ha arrancado\n"
                            + "en modo JSON (archivos .json en la carpeta '"
                            + config.getCarpetaDatos() + "').\n\n"
                            + "Detalle: " + e.getMessage(),
                    ButtonType.OK);
            aviso.setHeaderText("Base de datos no disponible");
            aviso.setTitle("Aviso");
            aviso.showAndWait();
        }
    }

    // ================================================================== //
    //                            NAVEGACION                               //
    // ================================================================== //

    public static void mostrarLogin() {
        try {
            Parent raiz = cargar("/fxml/login.fxml");
            Scene escena = new Scene(raiz, 760, 560);
            aplicarCss(escena);
            escenario.setScene(escena);
            escenario.centerOnScreen();
            escenario.show();
        } catch (Exception e) {
            errorFatal("No se pudo abrir la pantalla de acceso", e);
        }
    }

    /** Carga la pantalla principal y deja el panel de contenido listo. */
    public static void mostrarPrincipal() {
        try {
            FXMLLoader cargador = new FXMLLoader(Aplicacion.class.getResource("/fxml/principal.fxml"));
            Parent raiz = cargador.load();

            ControladorPrincipal controlador = cargador.getController();
            panelContenido = controlador.getPanelContenido();
            etiquetaEstado = controlador.getEtiquetaEstado();

            Scene escena = new Scene(raiz, 1280, 800);
            aplicarCss(escena);
            escenario.setScene(escena);
            escenario.centerOnScreen();

            controlador.saludoInicial();
        } catch (Exception e) {
            errorFatal("No se pudo abrir la pantalla principal", e);
        }
    }

    /** Sustituye el contenido central por la vista indicada (nombre del FXML). */
    public static void cargarVista(String fxml) {
        if (panelContenido == null) {
            return;
        }
        try {
            FXMLLoader cargador = new FXMLLoader(Aplicacion.class.getResource("/fxml/" + fxml));
            Parent vista = cargador.load();
            panelContenido.getChildren().setAll(vista);
            if (etiquetaEstado != null) {
                etiquetaEstado.setText("Vista: " + fxml.replace(".fxml", "") + "  ·  "
                        + java.time.LocalDateTime.now()
                        .format(com.inventario.util.UtilFechas.HORA));
            }
        } catch (Exception e) {
            errorFatal("No se pudo cargar la vista " + fxml, e);
        }
    }

    public static void estado(String mensaje) {
        if (etiquetaEstado != null) {
            etiquetaEstado.setText(mensaje);
        }
    }

    private static Parent cargar(String recurso) throws Exception {
        return new FXMLLoader(Aplicacion.class.getResource(recurso)).load();
    }

    private static void aplicarCss(Scene escena) {
        var css = Aplicacion.class.getResource(CSS);
        if (css != null) {
            escena.getStylesheets().add(css.toExternalForm());
        }
    }

    private static void errorFatal(String mensaje, Exception e) {
        e.printStackTrace();
        Alert alerta = new Alert(Alert.AlertType.ERROR,
                mensaje + "\n\n" + e.getMessage(), ButtonType.OK);
        alerta.setTitle("Error");
        alerta.setHeaderText("Error fatal");
        alerta.showAndWait();
    }

    // ================================================================== //

    @Override
    public void stop() {
        Sesion.cerrar();
        com.inventario.db.ConexionMySQL.cerrar();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }

    /** Utilidad para otros puntos del codigo: la carpeta de datos activa. */
    public static Path carpetaDatos() {
        return AppContexto.config().getDirectorioDatos();
    }

    /** Icono de la aplicacion (si se anade /icono.png a los recursos). */
    public static Image icono() {
        var recurso = Aplicacion.class.getResource("/icono.png");
        return recurso == null ? null : new Image(recurso.toExternalForm());
    }
}
