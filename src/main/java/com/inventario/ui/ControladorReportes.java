package com.inventario.ui;

import com.inventario.Aplicacion;
import com.inventario.AppContexto;
import com.inventario.util.UtilFechas;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

/** Generacion de informes en PDF y Excel. */
public class ControladorReportes {

    @FXML
    private DatePicker fechaDesde;
    @FXML
    private DatePicker fechaHasta;
    @FXML
    private TableView<File> tablaInformes;
    @FXML
    private TableColumn<File, String> colInforme;
    @FXML
    private TableColumn<File, String> colFechaInforme;

    @FXML
    private void initialize() {
        fechaDesde.setValue(LocalDate.now().withDayOfMonth(1));
        fechaHasta.setValue(LocalDate.now());

        colInforme.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));
        colFechaInforme.setCellValueFactory(cd -> new SimpleStringProperty(fechaArchivo(cd.getValue())));

        tablaInformes.setRowFactory(fila -> {
            var filaTabla = new javafx.scene.control.TableRow<File>();
            filaTabla.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !filaTabla.isEmpty()) {
                    abrirArchivo(filaTabla.getItem());
                }
            });
            return filaTabla;
        });

        cargarInformes();
    }

    // ================================================================== //
    //                        GENERAR INFORMES                             //
    // ================================================================== //

    @FXML
    private void pdfInventario() {
        generar(() -> AppContexto.reportes().pdfInventario(AppContexto.reportes().carpetaInformes()));
    }

    @FXML
    private void excelInventario() {
        generar(() -> AppContexto.reportes().excelInventario(AppContexto.reportes().carpetaInformes()));
    }

    @FXML
    private void pdfMovimientos() {
        generar(() -> AppContexto.reportes()
                .pdfMovimientos(AppContexto.reportes().carpetaInformes(),
                        fechaDesde.getValue(), fechaHasta.getValue()));
    }

    @FXML
    private void excelMovimientos() {
        generar(() -> AppContexto.reportes()
                .excelMovimientos(AppContexto.reportes().carpetaInformes(),
                        fechaDesde.getValue(), fechaHasta.getValue()));
    }

    @FXML
    private void pdfStockBajo() {
        generar(() -> AppContexto.reportes().pdfStockBajo(AppContexto.reportes().carpetaInformes()));
    }

    @FXML
    private void excelStockBajo() {
        generar(() -> AppContexto.reportes().excelStockBajo(AppContexto.reportes().carpetaInformes()));
    }

    @FXML
    private void pdfEtiquetas() {
        generar(() -> AppContexto.reportes().pdfEtiquetas(AppContexto.reportes().carpetaEtiquetas()));
    }

    private void generar(Generador accion) {
        try {
            File archivo = accion.generar();
            cargarInformes();
            Aplicacion.estado("Informe generado: " + archivo.getName());
            Alertas.info("Informe generado",
                    archivo.getName() + "\n\nCarpeta:\n" + archivo.getAbsolutePath());
        } catch (Exception e) {
            Alertas.excepcion("No se pudo generar el informe", e);
        }
    }

    @FXML
    private void abrirCarpeta() {
        File carpeta = AppContexto.reportes().carpetaInformes().toFile();
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        try {
            Desktop.getDesktop().open(carpeta);
        } catch (Exception e) {
            Alertas.info("Carpeta de informes", carpeta.getAbsolutePath());
        }
    }

    private void abrirArchivo(File archivo) {
        try {
            Desktop.getDesktop().open(archivo);
        } catch (Exception e) {
            Alertas.info("Archivo", archivo.getAbsolutePath());
        }
    }

    /** Fecha de modificacion del informe, en texto. */
    private static String fechaArchivo(File archivo) {
        try {
            return UtilFechas.formatearFecha(java.time.LocalDateTime.ofInstant(
                    java.nio.file.Files.getLastModifiedTime(archivo.toPath()).toInstant(),
                    java.time.ZoneId.systemDefault()));
        } catch (Exception e) {
            return "";
        }
    }

    private void cargarInformes() {
        List<File> archivos = AppContexto.reportes().informesGenerados();
        tablaInformes.getItems().setAll(archivos);
    }

    /** Permite pasar la operacion de generacion como referencia de metodo. */
    @FunctionalInterface
    private interface Generador {
        File generar() throws Exception;
    }
}
