package com.inventario.ui;

import com.inventario.Aplicacion;
import com.inventario.AppContexto;
import com.inventario.excepcion.ExcepcionNegocio;
import com.inventario.modelo.Movimiento;
import com.inventario.modelo.Producto;
import com.inventario.modelo.TipoMovimiento;
import com.inventario.util.UtilFechas;
import com.inventario.util.UtilTextos;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

/** Registro de entradas, salidas y ajustes de stock + historial. */
public class ControladorStock {

    private static final String TODOS = "Todos";

    @FXML
    private ComboBox<Producto> comboProducto;
    @FXML
    private ComboBox<TipoMovimiento> comboTipo;
    @FXML
    private TextField campoCantidad;
    @FXML
    private Label etiquetaCantidad;
    @FXML
    private TextField campoMotivo;
    @FXML
    private Label etiquetaStockActual;
    @FXML
    private TextField campoBusqueda;
    @FXML
    private ComboBox<String> comboTipoFiltro;
    @FXML
    private TableView<Movimiento> tabla;
    @FXML
    private TableColumn<Movimiento, String> colFecha;
    @FXML
    private TableColumn<Movimiento, String> colTipo;
    @FXML
    private TableColumn<Movimiento, String> colProducto;
    @FXML
    private TableColumn<Movimiento, Number> colCantidad;
    @FXML
    private TableColumn<Movimiento, Number> colAnterior;
    @FXML
    private TableColumn<Movimiento, Number> colResultante;
    @FXML
    private TableColumn<Movimiento, String> colMotivo;
    @FXML
    private TableColumn<Movimiento, String> colUsuario;
    @FXML
    private Label etiquetaContador;

    @FXML
    private void initialize() {
        comboTipo.setItems(FXCollections.observableArrayList(TipoMovimiento.values()));
        comboTipo.getSelectionModel().select(TipoMovimiento.ENTRADA);

        comboTipoFiltro.setItems(FXCollections.observableArrayList(
                TODOS, "Entrada", "Salida", "Ajuste"));
        comboTipoFiltro.getSelectionModel().selectFirst();

        comboProducto.setOnAction(e -> mostrarStockSeleccionado());
        comboTipo.setOnAction(e -> {
            TipoMovimiento tipo = comboTipo.getValue();
            if (tipo == TipoMovimiento.SALIDA) {
                campoMotivo.setPromptText("Venta, merma, rotura...");
                etiquetaCantidad.setText("Cantidad:");
                campoCantidad.setPromptText("unidades que salen");
            } else if (tipo == TipoMovimiento.AJUSTE) {
                campoMotivo.setPromptText("Inventario fisico, correccion...");
                etiquetaCantidad.setText("Stock contado:");
                campoCantidad.setPromptText("stock real");
            } else {
                campoMotivo.setPromptText("Compra, devolucion...");
                etiquetaCantidad.setText("Cantidad:");
                campoCantidad.setPromptText("unidades que entran");
            }
        });

        colFecha.setCellValueFactory(cd -> new SimpleStringProperty(
                UtilFechas.formatear(cd.getValue().getFecha())));
        colTipo.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTipo() == null ? "" : cd.getValue().getTipo().getEtiqueta()));
        colProducto.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getProductoNombre()));
        colCantidad.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getCantidad()));
        colAnterior.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getStockAnterior()));
        colResultante.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getStockResultante()));
        colMotivo.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getMotivo() == null ? "" : cd.getValue().getMotivo()));
        colUsuario.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getUsuarioNombre() == null ? "" : cd.getValue().getUsuarioNombre()));

        cargar();
    }

    // ================================================================== //
    //                      REGISTRO DE MOVIMIENTOS                       //
    // ================================================================== //

    @FXML
    private void registrar() {
        Producto producto = comboProducto.getValue();
        if (producto == null) {
            Alertas.aviso("Selecciona un producto.");
            return;
        }
        TipoMovimiento tipo = comboTipo.getValue();
        int cantidad;
        try {
            cantidad = Integer.parseInt(campoCantidad.getText().trim());
        } catch (NumberFormatException e) {
            Alertas.error("La cantidad debe ser un numero entero.");
            campoCantidad.requestFocus();
            return;
        }

        try {
            Movimiento movimiento = AppContexto.inventario()
                    .registrarMovimiento(tipo, producto.getId(), cantidad, campoMotivo.getText());

            campoCantidad.clear();
            campoMotivo.clear();
            cargar();
            comboProducto.getSelectionModel().select(
                    comboProducto.getItems().stream()
                            .filter(p -> p.getId().equals(producto.getId()))
                            .findFirst().orElse(null));
            mostrarStockSeleccionado();

            String mensaje = tipo.getEtiqueta().toUpperCase() + " registrada: " + cantidad + " "
                    + producto.getUnidad() + " de " + producto.getNombre()
                    + "  (stock " + movimiento.getStockAnterior() + " -> "
                    + movimiento.getStockResultante() + ")";
            Aplicacion.estado(mensaje);
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    private void mostrarStockSeleccionado() {
        Producto producto = comboProducto.getValue();
        if (producto == null) {
            etiquetaStockActual.setText("Selecciona un producto para ver su stock actual");
            return;
        }
        etiquetaStockActual.setText("Stock actual de \"" + producto.getNombre() + "\": "
                + producto.getStock() + " " + producto.getUnidad()
                + "   ·   minimo " + producto.getStockMinimo());
    }

    // ================================================================== //
    //                          CONSULTA DEL HISTORIAL                     //
    // ================================================================== //

    @FXML
    private void buscar() {
        cargar();
    }

    @FXML
    private void filtrar() {
        cargar();
    }

    @FXML
    private void actualizar() {
        campoBusqueda.clear();
        comboTipoFiltro.getSelectionModel().selectFirst();
        cargar();
        Aplicacion.estado("Historial de movimientos actualizado");
    }

    private void cargar() {
        List<Producto> productos = AppContexto.inventario()
                .listarProductos(null, null, false);

        Producto seleccionado = comboProducto.getValue();
        comboProducto.setItems(FXCollections.observableArrayList(productos));
        if (seleccionado != null) {
            productos.stream()
                    .filter(p -> seleccionado.getId().equals(p.getId()))
                    .findFirst()
                    .ifPresent(comboProducto.getSelectionModel()::select);
        }

        TipoMovimiento filtroTipo = null;
        String textoTipo = comboTipoFiltro.getValue();
        if (textoTipo != null && !TODOS.equals(textoTipo)) {
            filtroTipo = TipoMovimiento.valueOf(textoTipo.toUpperCase());
        }

        List<Movimiento> movimientos = AppContexto.inventario()
                .listarMovimientos(campoBusqueda.getText(), filtroTipo, null);

        tabla.getItems().setAll(movimientos);
        etiquetaContador.setText(movimientos.size() + " movimientos");
    }
}
