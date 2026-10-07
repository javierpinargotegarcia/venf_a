package com.inventario.ui;

import com.inventario.Aplicacion;
import com.inventario.AppContexto;
import com.inventario.excepcion.ExcepcionNegocio;
import com.inventario.modelo.Categoria;
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

/** CRUD de categorias de mercaderia. */
public class ControladorCategorias {

    @FXML
    private TextField campoBusqueda;
    @FXML
    private TableView<Categoria> tabla;
    @FXML
    private TableColumn<Categoria, Number> colId;
    @FXML
    private TableColumn<Categoria, String> colNombre;
    @FXML
    private TableColumn<Categoria, String> colDescripcion;
    @FXML
    private TableColumn<Categoria, Number> colProductos;
    @FXML
    private Label etiquetaContador;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getId().intValue()));
        colNombre.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNombre()));
        colDescripcion.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDescripcion() == null ? "" : cd.getValue().getDescripcion()));
        colProductos.setCellValueFactory(cd -> new SimpleIntegerProperty(
                (int) AppContexto.inventario().contarProductosDeCategoria(cd.getValue().getId())));

        tabla.setRowFactory(fila -> {
            var filaTabla = new javafx.scene.control.TableRow<Categoria>();
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
        Aplicacion.estado("Lista de categorias actualizada");
    }

    @FXML
    private void nueva() {
        Categoria categoria = new Categoria();
        Optional<Categoria> datos = pedirDatos(categoria);
        if (datos.isEmpty()) {
            return;
        }
        try {
            AppContexto.inventario().guardarCategoria(datos.get());
            cargar();
            Aplicacion.estado("Categoria creada: " + datos.get().getNombre());
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    @FXML
    private void editar() {
        Categoria categoria = seleccionada();
        if (categoria == null) {
            return;
        }
        Optional<Categoria> datos = pedirDatos(categoria);
        if (datos.isEmpty()) {
            return;
        }
        try {
            AppContexto.inventario().guardarCategoria(datos.get());
            cargar();
            Aplicacion.estado("Categoria actualizada: " + datos.get().getNombre());
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        Categoria categoria = seleccionada();
        if (categoria == null) {
            return;
        }
        if (!Alertas.confirmar("Eliminar categoria",
                "Vas a eliminar la categoria \"" + categoria.getNombre() + "\".")) {
            return;
        }
        try {
            AppContexto.inventario().eliminarCategoria(categoria.getId());
            cargar();
            Aplicacion.estado("Categoria eliminada: " + categoria.getNombre());
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //

    private void cargar() {
        String texto = campoBusqueda == null ? "" : campoBusqueda.getText();
        List<Categoria> todas = AppContexto.inventario().listarCategorias().stream()
                .filter(c -> texto == null || texto.isBlank()
                        || com.inventario.util.UtilTextos.contiene(c.getNombre(), texto)
                        || com.inventario.util.UtilTextos.contiene(c.getDescripcion(), texto))
                .toList();
        tabla.getItems().setAll(todas);
        etiquetaContador.setText(todas.size() + " categorias");
    }

    private Categoria seleccionada() {
        Categoria categoria = tabla.getSelectionModel().getSelectedItem();
        if (categoria == null) {
            Alertas.aviso("Selecciona primero una categoria de la lista.");
        }
        return categoria;
    }

    private Optional<Categoria> pedirDatos(Categoria categoria) {
        boolean esNueva = categoria.getId() == null;

        Dialog<Categoria> dialogo = new Dialog<>();
        dialogo.setTitle(esNueva ? "Nueva categoria" : "Editar categoria");
        dialogo.setHeaderText(esNueva ? "Alta de categoria" : categoria.getNombre());
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField campoNombre = new TextField(categoria.getNombre());
        campoNombre.setPromptText("Ej. Bebidas");
        TextField campoDescripcion = new TextField(categoria.getDescripcion());
        campoDescripcion.setPromptText("Descripcion opcional");

        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(9);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(110);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setPrefWidth(320);
        rejilla.getColumnConstraints().addAll(c1, c2);
        agregarFila(rejilla, 0, "Nombre *", campoNombre);
        agregarFila(rejilla, 1, "Descripcion", campoDescripcion);

        VBox raiz = new VBox(rejilla);
        dialogo.getDialogPane().setContent(raiz);

        Categoria[] resultado = new Categoria[1];
        Node botonOk = dialogo.getDialogPane().lookupButton(ButtonType.OK);
        botonOk.addEventFilter(ActionEvent.ACTION, evento -> {
            if (campoNombre.getText() == null || campoNombre.getText().isBlank()) {
                Alertas.error("El nombre de la categoria es obligatorio.");
                evento.consume();
                return;
            }
            Categoria construida = new Categoria();
            construida.setId(categoria.getId());
            construida.setNombre(campoNombre.getText().trim());
            construida.setDescripcion(campoDescripcion.getText());
            resultado[0] = construida;
        });

        dialogo.setResultConverter(boton -> boton == ButtonType.OK ? resultado[0] : null);
        return dialogo.showAndWait();
    }

    private static void agregarFila(GridPane rejilla, int fila, String etiqueta, Node campo) {
        Label texto = new Label(etiqueta);
        texto.setStyle("-fx-font-weight:bold; -fx-text-fill:#44566c;");
        GridPane.setHalignment(texto, javafx.geometry.HPos.RIGHT);
        rejilla.add(texto, 0, fila);
        rejilla.add(campo, 1, fila);
    }
}
