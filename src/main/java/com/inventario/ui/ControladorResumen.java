package com.inventario.ui;

import com.inventario.AppContexto;
import com.inventario.modelo.Movimiento;
import com.inventario.modelo.Producto;
import com.inventario.util.UtilFechas;
import com.inventario.util.UtilTextos;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** Panel de control con las cifras clave del inventario. */
public class ControladorResumen {

    @FXML
    private Label kpiProductos;
    @FXML
    private Label kpiUnidades;
    @FXML
    private Label kpiValor;
    @FXML
    private Label kpiStockBajo;
    @FXML
    private Label kpiMovimientos;
    @FXML
    private Label etiquetaStockBajo;

    @FXML
    private TableView<Producto> tablaStockBajo;
    @FXML
    private TableColumn<Producto, String> colBajoCodigo;
    @FXML
    private TableColumn<Producto, String> colBajoProducto;
    @FXML
    private TableColumn<Producto, Number> colBajoStock;
    @FXML
    private TableColumn<Producto, Number> colBajoMinimo;
    @FXML
    private TableColumn<Producto, Number> colBajoSugerido;

    @FXML
    private TableView<Movimiento> tablaMovimientos;
    @FXML
    private TableColumn<Movimiento, String> colMovFecha;
    @FXML
    private TableColumn<Movimiento, String> colMovTipo;
    @FXML
    private TableColumn<Movimiento, String> colMovProducto;
    @FXML
    private TableColumn<Movimiento, Number> colMovCantidad;
    @FXML
    private TableColumn<Movimiento, Number> colMovResultado;
    @FXML
    private TableColumn<Movimiento, String> colMovMotivo;

    @FXML
    private void initialize() {
        definirColumnas();
        cargar();
    }

    private void definirColumnas() {
        colBajoCodigo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCodigoBarras()));
        colBajoProducto.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNombre()));
        colBajoStock.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getStock()));
        colBajoMinimo.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getStockMinimo()));
        colBajoSugerido.setCellValueFactory(cd -> new SimpleIntegerProperty(
                Math.max(cd.getValue().getStockMinimo() * 2 - cd.getValue().getStock(), 1)));

        colMovFecha.setCellValueFactory(cd -> new SimpleStringProperty(
                UtilFechas.formatear(cd.getValue().getFecha())));
        colMovTipo.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTipo() == null ? "" : cd.getValue().getTipo().getEtiqueta()));
        colMovProducto.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getProductoNombre()));
        colMovCantidad.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getCantidad()));
        colMovResultado.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getStockResultante()));
        colMovMotivo.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getMotivo() == null ? "" : cd.getValue().getMotivo()));
    }

    private void cargar() {
        var inventario = AppContexto.inventario();
        var resumen = inventario.resumen();

        kpiProductos.setText(String.valueOf(resumen.productosActivos()));
        kpiUnidades.setText(String.valueOf(resumen.unidadesTotales()));
        kpiValor.setText(UtilTextos.dinero(resumen.valorInventario()) + " €");
        kpiStockBajo.setText(String.valueOf(resumen.productosStockBajo()));
        kpiMovimientos.setText(String.valueOf(resumen.movimientosHoy()));

        var bajos = inventario.productosConStockBajo();
        tablaStockBajo.getItems().setAll(bajos);
        etiquetaStockBajo.setText(bajos.size() + " productos");

        var movimientos = inventario.listarMovimientos(null, null, null);
        tablaMovimientos.getItems().setAll(movimientos.stream().limit(15).toList());
    }
}
