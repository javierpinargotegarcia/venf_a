package com.inventario.ui;

import com.inventario.Aplicacion;
import com.inventario.AppContexto;
import com.inventario.excepcion.ExcepcionNegocio;
import com.inventario.modelo.RolUsuario;
import com.inventario.modelo.Usuario;
import com.inventario.seguridad.Sesion;
import com.inventario.util.UtilFechas;
import com.inventario.util.UtilTextos;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/** Gestion de usuarios del sistema (solo administradores). */
public class ControladorUsuarios {

    @FXML
    private TextField campoBusqueda;
    @FXML
    private TableView<Usuario> tabla;
    @FXML
    private TableColumn<Usuario, String> colUsuario;
    @FXML
    private TableColumn<Usuario, String> colNombre;
    @FXML
    private TableColumn<Usuario, String> colRol;
    @FXML
    private TableColumn<Usuario, String> colEstado;
    @FXML
    private TableColumn<Usuario, String> colRegistro;
    @FXML
    private Label etiquetaContador;

    @FXML
    private void initialize() {
        colUsuario.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUsuario()));
        colNombre.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNombreCompleto()));
        colRol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getRol() == null ? "" : cd.getValue().getRol().getEtiqueta()));
        colEstado.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().isActivo() ? "Activo" : "Desactivado"));
        colRegistro.setCellValueFactory(cd -> new SimpleStringProperty(
                UtilFechas.formatearFecha(cd.getValue().getFechaRegistro())));

        cargar();
    }

    @FXML
    private void buscar() {
        cargar();
    }

    @FXML
    private void actualizar() {
        campoBusqueda.clear();
        cargar();
        Aplicacion.estado("Lista de usuarios actualizada");
    }

    @FXML
    private void nuevo() {
        Usuario usuario = new Usuario();
        usuario.setRol(RolUsuario.OPERARIO);
        usuario.setActivo(true);

        Optional<DatosUsuario> datos = pedirDatos(usuario, true);
        if (datos.isEmpty()) {
            return;
        }
        try {
            Usuario creado = AppContexto.autenticacion()
                    .guardar(construirUsuario(usuario, datos.get()), datos.get().clave());
            cargar();
            Aplicacion.estado("Usuario creado: " + creado.getUsuario());
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    @FXML
    private void editar() {
        Usuario usuario = seleccionado();
        if (usuario == null) {
            return;
        }
        Optional<DatosUsuario> datos = pedirDatos(usuario, false);
        if (datos.isEmpty()) {
            return;
        }
        try {
            AppContexto.autenticacion().guardar(construirUsuario(usuario, datos.get()), datos.get().clave());
            cargar();
            Aplicacion.estado("Usuario actualizado: " + usuario.getUsuario());
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    @FXML
    private void activarDesactivar() {
        Usuario usuario = seleccionado();
        if (usuario == null) {
            return;
        }
        try {
            if (usuario.isActivo()) {
                if (!Alertas.confirmar("Desactivar usuario",
                        "Se desactivara el usuario \"" + usuario.getUsuario()
                                + "\" y no podra iniciar sesion.")) {
                    return;
                }
                AppContexto.autenticacion().desactivar(usuario.getId());
                Aplicacion.estado("Usuario desactivado: " + usuario.getUsuario());
            } else {
                AppContexto.autenticacion().activar(usuario.getId());
                Aplicacion.estado("Usuario activado: " + usuario.getUsuario());
            }
            cargar();
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //

    private void cargar() {
        String texto = campoBusqueda.getText();
        List<Usuario> lista = AppContexto.autenticacion().listar().stream()
                .filter(u -> texto == null || texto.isBlank()
                        || UtilTextos.contiene(u.getUsuario(), texto)
                        || UtilTextos.contiene(u.getNombreCompleto(), texto))
                .toList();
        tabla.getItems().setAll(lista);
        etiquetaContador.setText(lista.size() + " usuarios");
    }

    private Usuario seleccionado() {
        Usuario usuario = tabla.getSelectionModel().getSelectedItem();
        if (usuario == null) {
            Alertas.aviso("Selecciona primero un usuario de la lista.");
        }
        return usuario;
    }

    private static Usuario construirUsuario(Usuario original, DatosUsuario datos) {
        Usuario u = new Usuario();
        u.setId(original.getId());
        u.setFechaRegistro(original.getFechaRegistro());
        u.setUsuario(datos.usuario());
        u.setNombreCompleto(datos.nombre());
        u.setRol(datos.rol());
        u.setActivo(original.isActivo());
        return u;
    }

    // ------------------------------------------------------------------ //

    private record DatosUsuario(String usuario, String nombre, String clave, RolUsuario rol) {
    }

    private Optional<DatosUsuario> pedirDatos(Usuario usuario, boolean esNuevo) {
        Dialog<DatosUsuario> dialogo = new Dialog<>();
        dialogo.setTitle(esNuevo ? "Nuevo usuario" : "Editar usuario");
        dialogo.setHeaderText(esNuevo ? "Alta de usuario" : usuario.getUsuario());
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField campoUsuario = new TextField(usuario.getUsuario());
        TextField campoNombre = new TextField(usuario.getNombreCompleto());
        PasswordField campoClave = new PasswordField();
        campoClave.setPromptText(esNuevo
                ? "Minimo " + com.inventario.servicio.ServicioAutenticacion.LONGITUD_MINIMA_CLAVE
                + " caracteres (obligatorio)"
                : "Dejar vacio para no cambiarla");
        ComboBox<RolUsuario> comboRol = new ComboBox<>(
                javafx.collections.FXCollections.observableArrayList(RolUsuario.values()));
        comboRol.getSelectionModel().select(usuario.getRol());
        CheckBox checkActivo = new CheckBox("Usuario activo (puede iniciar sesion)");
        checkActivo.setSelected(usuario.isActivo());

        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(9);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setPrefWidth(300);
        rejilla.getColumnConstraints().addAll(c1, c2);

        int fila = 0;
        fila = agregarFila(rejilla, fila, "Usuario *", campoUsuario);
        fila = agregarFila(rejilla, fila, "Nombre completo *", campoNombre);
        fila = agregarFila(rejilla, fila, esNuevo ? "Contrasena *" : "Nueva contrasena", campoClave);
        fila = agregarFila(rejilla, fila, "Rol", comboRol);
        rejilla.add(checkActivo, 1, fila);

        VBox raiz = new VBox(rejilla);
        dialogo.getDialogPane().setContent(raiz);

        DatosUsuario[] resultado = new DatosUsuario[1];
        Node botonOk = dialogo.getDialogPane().lookupButton(ButtonType.OK);
        botonOk.addEventFilter(ActionEvent.ACTION, evento -> {
            String usuarioTexto = campoUsuario.getText() == null ? "" : campoUsuario.getText().trim();
            String nombre = campoNombre.getText() == null ? "" : campoNombre.getText().trim();
            String clave = campoClave.getText() == null ? "" : campoClave.getText();

            if (usuarioTexto.isEmpty()) {
                Alertas.error("El nombre de usuario es obligatorio.");
                evento.consume();
                return;
            }
            if (nombre.isEmpty()) {
                Alertas.error("El nombre completo es obligatorio.");
                evento.consume();
                return;
            }
            int minimo = com.inventario.servicio.ServicioAutenticacion.LONGITUD_MINIMA_CLAVE;
            if (esNuevo && clave.length() < minimo) {
                Alertas.error("La contrasena debe tener al menos " + minimo + " caracteres.");
                evento.consume();
                return;
            }
            if (!esNuevo && !clave.isBlank() && clave.length() < minimo) {
                Alertas.error("La contrasena debe tener al menos " + minimo + " caracteres.");
                evento.consume();
                return;
            }
            resultado[0] = new DatosUsuario(usuarioTexto, nombre,
                    clave.isBlank() ? null : clave, comboRol.getValue());
        });

        dialogo.setResultConverter(boton -> boton == ButtonType.OK ? resultado[0] : null);
        return dialogo.showAndWait();
    }

    private static int agregarFila(GridPane rejilla, int fila, String etiqueta, Node campo) {
        Label texto = new Label(etiqueta);
        texto.setStyle("-fx-font-weight:bold; -fx-text-fill:#44566c;");
        GridPane.setHalignment(texto, javafx.geometry.HPos.RIGHT);
        rejilla.add(texto, 0, fila);
        rejilla.add(campo, 1, fila);
        return fila + 1;
    }
}
