package com.inventario.ui;

import com.inventario.Aplicacion;
import com.inventario.AppContexto;
import com.inventario.excepcion.ExcepcionNegocio;
import com.inventario.modelo.Proveedor;
import com.inventario.util.UtilTextos;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/** CRUD de proveedores de mercaderia. */
public class ControladorProveedores {

    @FXML
    private TextField campoBusqueda;
    @FXML
    private TableView<Proveedor> tabla;
    @FXML
    private TableColumn<Proveedor, Number> colId;
    @FXML
    private TableColumn<Proveedor, String> colNombre;
    @FXML
    private TableColumn<Proveedor, String> colContacto;
    @FXML
    private TableColumn<Proveedor, String> colTelefono;
    @FXML
    private TableColumn<Proveedor, String> colEmail;
    @FXML
    private TableColumn<Proveedor, String> colDireccion;
    @FXML
    private TableColumn<Proveedor, Number> colProductos;
    @FXML
    private Label etiquetaContador;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getId().intValue()));
        colNombre.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNombre()));
        colContacto.setCellValueFactory(cd -> new SimpleStringProperty(
                nulo(cd.getValue().getContacto())));
        colTelefono.setCellValueFactory(cd -> new SimpleStringProperty(
                nulo(cd.getValue().getTelefono())));
        colEmail.setCellValueFactory(cd -> new SimpleStringProperty(nulo(cd.getValue().getEmail())));
        colDireccion.setCellValueFactory(cd -> new SimpleStringProperty(
                nulo(cd.getValue().getDireccion())));
        colProductos.setCellValueFactory(cd -> new SimpleIntegerProperty(
                (int) AppContexto.inventario().contarProductosDeProveedor(cd.getValue().getId())));

        tabla.setRowFactory(fila -> {
            var filaTabla = new javafx.scene.control.TableRow<Proveedor>();
            filaTabla.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !filaTabla.isEmpty()) {
                    editar();
                }
            });
            return filaTabla;
        });

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
        Aplicacion.estado("Lista de proveedores actualizada");
    }

    @FXML
    private void nuevo() {
        Optional<Proveedor> datos = pedirDatos(new Proveedor());
        if (datos.isEmpty()) {
            return;
        }
        try {
            AppContexto.inventario().guardarProveedor(datos.get());
            cargar();
            Aplicacion.estado("Proveedor creado: " + datos.get().getNombre());
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    @FXML
    private void editar() {
        Proveedor proveedor = seleccionado();
        if (proveedor == null) {
            return;
        }
        Optional<Proveedor> datos = pedirDatos(proveedor);
        if (datos.isEmpty()) {
            return;
        }
        try {
            AppContexto.inventario().guardarProveedor(datos.get());
            cargar();
            Aplicacion.estado("Proveedor actualizado: " + datos.get().getNombre());
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        Proveedor proveedor = seleccionado();
        if (proveedor == null) {
            return;
        }
        if (!Alertas.confirmar("Eliminar proveedor",
                "Vas a eliminar el proveedor \"" + proveedor.getNombre() + "\".")) {
            return;
        }
        try {
            AppContexto.inventario().eliminarProveedor(proveedor.getId());
            cargar();
            Aplicacion.estado("Proveedor eliminado: " + proveedor.getNombre());
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //

    private void cargar() {
        String texto = campoBusqueda.getText();
        List<Proveedor> lista = AppContexto.inventario().listarProveedores().stream()
                .filter(p -> texto == null || texto.isBlank()
                        || UtilTextos.contiene(p.getNombre(), texto)
                        || UtilTextos.contiene(p.getContacto(), texto)
                        || UtilTextos.contiene(p.getEmail(), texto)
                        || UtilTextos.contiene(p.getTelefono(), texto))
                .toList();
        tabla.getItems().setAll(lista);
        etiquetaContador.setText(lista.size() + " proveedores");
    }

    private Proveedor seleccionado() {
        Proveedor proveedor = tabla.getSelectionModel().getSelectedItem();
        if (proveedor == null) {
            Alertas.aviso("Selecciona primero un proveedor de la lista.");
        }
        return proveedor;
    }

    private Optional<Proveedor> pedirDatos(Proveedor proveedor) {
        boolean esNuevo = proveedor.getId() == null;

        Dialog<Proveedor> dialogo = new Dialog<>();
        dialogo.setTitle(esNuevo ? "Nuevo proveedor" : "Editar proveedor");
        dialogo.setHeaderText(esNuevo ? "Alta de proveedor" : proveedor.getNombre());
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField campoNombre = new TextField(proveedor.getNombre());
        TextField campoContacto = new TextField(proveedor.getContacto());
        TextField campoTelefono = new TextField(proveedor.getTelefono());
        TextField campoEmail = new TextField(proveedor.getEmail());
        TextField campoDireccion = new TextField(proveedor.getDireccion());

        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(9);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(110);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setPrefWidth(330);
        rejilla.getColumnConstraints().addAll(c1, c2);

        int fila = 0;
        fila = agregarFila(rejilla, fila, "Nombre *", campoNombre);
        fila = agregarFila(rejilla, fila, "Persona de contacto", campoContacto);
        fila = agregarFila(rejilla, fila, "Telefono", campoTelefono);
        fila = agregarFila(rejilla, fila, "Email", campoEmail);
        agregarFila(rejilla, fila, "Direccion", campoDireccion);

        VBox raiz = new VBox(rejilla);
        dialogo.getDialogPane().setContent(raiz);

        Proveedor[] resultado = new Proveedor[1];
        Node botonOk = dialogo.getDialogPane().lookupButton(ButtonType.OK);
        botonOk.addEventFilter(ActionEvent.ACTION, evento -> {
            if (campoNombre.getText() == null || campoNombre.getText().isBlank()) {
                Alertas.error("El nombre del proveedor es obligatorio.");
                evento.consume();
                return;
            }
            String email = campoEmail.getText();
            if (email != null && !email.isBlank()
                    && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                Alertas.error("El email no es valido.");
                evento.consume();
                return;
            }
            Proveedor construido = new Proveedor();
            construido.setId(proveedor.getId());
            construido.setNombre(campoNombre.getText().trim());
            construido.setContacto(campoContacto.getText());
            construido.setTelefono(campoTelefono.getText());
            construido.setEmail(email);
            construido.setDireccion(campoDireccion.getText());
            resultado[0] = construido;
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

    private static String nulo(String valor) {
        return valor == null ? "" : valor;
    }
}
