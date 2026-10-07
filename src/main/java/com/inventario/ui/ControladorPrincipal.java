package com.inventario.ui;

import com.inventario.Aplicacion;
import com.inventario.AppContexto;
import com.inventario.config.ConfigApp;
import com.inventario.seguridad.Sesion;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/** Pantalla contenedora: barra superior, menu lateral y area de contenido. */
public class ControladorPrincipal {

    @FXML
    private StackPane panelContenido;
    @FXML
    private Label etiquetaUsuario;
    @FXML
    private Label etiquetaModo;
    @FXML
    private Label etiquetaEstado;
    @FXML
    private Label etiquetaRuta;
    @FXML
    private Button btnUsuarios;
    @FXML
    private Button btnMigrar;

    public StackPane getPanelContenido() {
        return panelContenido;
    }

    public Label getEtiquetaEstado() {
        return etiquetaEstado;
    }

    @FXML
    private void initialize() {
        boolean admin = Sesion.esAdmin();

        // Gestion de usuarios y migracion: solo administradores
        btnUsuarios.setVisible(admin);
        btnUsuarios.setManaged(admin);
        btnMigrar.setVisible(admin);
        btnMigrar.setManaged(admin);

        String rol = Sesion.getUsuario() == null || Sesion.getUsuario().getRol() == null
                ? "" : " · " + Sesion.getUsuario().getRol();
        etiquetaUsuario.setText(Sesion.getNombreVisible() + rol);

        refrescarEstadoAlmacenamiento();
    }

    /** Carga la primera vista tras iniciar sesion. */
    public void saludoInicial() {
        irResumen();
    }

    private void refrescarEstadoAlmacenamiento() {
        ConfigApp config = AppContexto.config();
        boolean mysql = config.esModoMysql();

        etiquetaModo.setText(mysql ? "MySQL" : "JSON");
        etiquetaModo.getStyleClass().remove("etiqueta-mysql");
        if (mysql) {
            etiquetaModo.getStyleClass().add("etiqueta-mysql");
        }

        if (mysql) {
            btnMigrar.setText("Volver a modo JSON");
            etiquetaRuta.setText("Base de datos: " + config.getMysqlUrl());
        } else {
            btnMigrar.setText("Migrar JSON -> MySQL");
            etiquetaRuta.setText("Repositorio: " + config.getCarpetaDatos()
                    + "/productos.json, categorias.json, ...");
        }
    }

    // ================================================================== //
    //                           NAVEGACION                                //
    // ================================================================== //

    @FXML
    private void irResumen() {
        Aplicacion.cargarVista("resumen.fxml");
    }

    @FXML
    private void irProductos() {
        Aplicacion.cargarVista("productos.fxml");
    }

    @FXML
    private void irStock() {
        Aplicacion.cargarVista("stock.fxml");
    }

    @FXML
    private void irCategorias() {
        Aplicacion.cargarVista("categorias.fxml");
    }

    @FXML
    private void irProveedores() {
        Aplicacion.cargarVista("proveedores.fxml");
    }

    @FXML
    private void irReportes() {
        Aplicacion.cargarVista("reportes.fxml");
    }

    @FXML
    private void irUsuarios() {
        Aplicacion.cargarVista("usuarios.fxml");
    }

    // ================================================================== //
    //                         MIGRACION A MySQL                           //
    // ================================================================== //

    @FXML
    private void migrar() {
        if (!Sesion.esAdmin()) {
            Alertas.error("Solo un administrador puede cambiar el almacenamiento de datos.");
            return;
        }

        if (!AppContexto.config().esModoMysql()) {
            migrarJsonAMysql();
        } else {
            volverAModoJson();
        }
    }

    private void migrarJsonAMysql() {
        boolean continuar = Alertas.confirmar("Migrar a MySQL",
                "Se copiaran TODOS los datos del repositorio JSON\n"
                        + "(" + AppContexto.config().getCarpetaDatos() + "/)\n"
                        + "a la base de datos MySQL.\n\n"
                        + "Los archivos JSON no se borran: quedan como copia de seguridad.");
        if (!continuar) {
            return;
        }

        try {
            AppContexto.migracion().probarConexion();
        } catch (Exception e) {
            Alertas.error("No se pudo conectar con MySQL",
                    e.getMessage() + "\n\nRevisa el fichero config.properties "
                            + "(mysql.url, mysql.usuario, mysql.password).");
            return;
        }

        try {
            var resumen = AppContexto.migracion().migrarJsonAMysql();
            StringBuilder texto = new StringBuilder();
            texto.append("Servidor: ").append(resumen.servidor()).append("\n\n");
            texto.append("Categorias:   ").append(resumen.categorias()).append("\n");
            texto.append("Proveedores:  ").append(resumen.proveedores()).append("\n");
            texto.append("Usuarios:     ").append(resumen.usuarios()).append("\n");
            texto.append("Productos:    ").append(resumen.productos()).append("\n");
            texto.append("Movimientos:  ").append(resumen.movimientos()).append("\n");
            texto.append("\nTotal de registros: ").append(resumen.total());

            refrescarEstadoAlmacenamiento();
            irResumen();
            Alertas.info("Migracion completada", texto.toString());
            Aplicacion.estado("Migracion a MySQL completada: " + resumen.total() + " registros");
        } catch (Exception e) {
            Alertas.error("La migracion ha fallado", e.getMessage());
            Aplicacion.estado("Migracion cancelada: la aplicacion sigue en modo JSON");
        }
    }

    private void volverAModoJson() {
        boolean continuar = Alertas.confirmar("Volver a modo JSON",
                "La aplicacion dejara de usar MySQL y volvera a leer\n"
                        + "los archivos .json de la carpeta '" + AppContexto.config().getCarpetaDatos() + "/'.\n\n"
                        + "Aviso: esos archivos NO contienen los cambios hechos\n"
                        + "desde que se migro a MySQL.");
        if (!continuar) {
            return;
        }
        AppContexto.migracion().volverAModoJson();
        refrescarEstadoAlmacenamiento();
        irResumen();
        Aplicacion.estado("Almacenamiento activo: archivos JSON");
    }

    // ================================================================== //

    @FXML
    private void cerrarSesion() {
        boolean salir = Alertas.confirmar("Cerrar sesion",
                "Se cerrara la sesion de " + Sesion.getNombreVisible() + ".\nDeseas continuar?");
        if (!salir) {
            return;
        }
        AppContexto.autenticacion().cerrarSesion();
        Aplicacion.mostrarLogin();
    }
}
