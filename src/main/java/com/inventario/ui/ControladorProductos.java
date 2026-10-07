package com.inventario.ui;

import com.inventario.Aplicacion;
import com.inventario.AppContexto;
import com.inventario.excepcion.ExcepcionNegocio;
import com.inventario.modelo.Categoria;
import com.inventario.modelo.Producto;
import com.inventario.modelo.Proveedor;
import com.inventario.util.UtilTextos;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Alta, edicion, consulta y borrado de productos. */
public class ControladorProductos {

    private static final String[] UNIDADES = {"UND", "KG", "L", "CAJA", "PACK", "BOLSA"};

    @FXML
    private TextField campoBusqueda;
    @FXML
    private ComboBox<Categoria> comboCategoria;
    @FXML
    private CheckBox checkInactivos;
    @FXML
    private TableView<Producto> tabla;
    @FXML
    private TableColumn<Producto, String> colCodigo;
    @FXML
    private TableColumn<Producto, String> colNombre;
    @FXML
    private TableColumn<Producto, String> colCategoria;
    @FXML
    private TableColumn<Producto, String> colProveedor;
    @FXML
    private TableColumn<Producto, Number> colStock;
    @FXML
    private TableColumn<Producto, Number> colMinimo;
    @FXML
    private TableColumn<Producto, String> colCompra;
    @FXML
    private TableColumn<Producto, String> colVenta;
    @FXML
    private TableColumn<Producto, String> colValor;
    @FXML
    private TableColumn<Producto, String> colEstado;
    @FXML
    private Label etiquetaContador;

    private Map<Long, Categoria> mapaCategorias = Map.of();
    private Map<Long, Proveedor> mapaProveedores = Map.of();
    private Categoria todas = new Categoria(null, "Todas las categorias", null);
    /** Evita el bucle cargar() -> filtrar() -> cargar() al refrescar los combos. */
    private boolean cargando;

    @FXML
    private void initialize() {
        comboCategoria.getItems().setAll(todas);
        comboCategoria.getSelectionModel().selectFirst();

        colCodigo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCodigoBarras()));
        colNombre.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getNombre() + (cd.getValue().isActivo() ? "" : "  (baja)")));
        colCategoria.setCellValueFactory(cd -> new SimpleStringProperty(
                AppContexto.inventario().nombreCategoria(cd.getValue().getCategoriaId(), mapaCategorias)));
        colProveedor.setCellValueFactory(cd -> new SimpleStringProperty(
                AppContexto.inventario().nombreProveedor(cd.getValue().getProveedorId(), mapaProveedores)));
        colStock.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getStock()));
        colMinimo.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getStockMinimo()));
        colCompra.setCellValueFactory(cd -> new SimpleStringProperty(
                UtilTextos.dinero(cd.getValue().getPrecioCompra())));
        colVenta.setCellValueFactory(cd -> new SimpleStringProperty(
                UtilTextos.dinero(cd.getValue().getPrecioVenta())));
        colValor.setCellValueFactory(cd -> new SimpleStringProperty(
                UtilTextos.dinero(cd.getValue().getValorStock())));
        colEstado.setCellValueFactory(cd -> new SimpleStringProperty(
                !cd.getValue().isActivo() ? "DE BAJA"
                        : cd.getValue().isStockBajo() ? "STOCK BAJO" : "OK"));

        // Doble clic para editar
        tabla.setRowFactory(fila -> {
            var filaTabla = new javafx.scene.control.TableRow<Producto>();
            filaTabla.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !filaTabla.isEmpty()) {
                    editar();
                }
            });
            return filaTabla;
        });

        cargar();
    }

    // ================================================================== //
    //                          ACCIONES                                   //
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
        comboCategoria.getSelectionModel().selectFirst();
        checkInactivos.setSelected(false);
        cargar();
        Aplicacion.estado("Lista de productos actualizada");
    }

    @FXML
    private void nuevo() {
        Producto producto = new Producto();
        producto.setCodigoBarras(AppContexto.codigos().siguienteCodigo());
        producto.setUnidad("UND");
        producto.setStockMinimo(5);
        producto.setActivo(true);

        Optional<Producto> datos = pedirDatosProducto(producto);
        if (datos.isEmpty()) {
            return;
        }
        try {
            AppContexto.inventario().guardarProducto(datos.get());
            cargar();
            Aplicacion.estado("Producto creado: " + datos.get().getNombre());
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    @FXML
    private void editar() {
        Producto producto = seleccionado();
        if (producto == null) {
            return;
        }
        Optional<Producto> datos = pedirDatosProducto(producto);
        if (datos.isEmpty()) {
            return;
        }
        try {
            AppContexto.inventario().guardarProducto(datos.get());
            cargar();
            Aplicacion.estado("Producto actualizado: " + datos.get().getNombre());
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        Producto producto = seleccionado();
        if (producto == null) {
            return;
        }
        boolean confirmar = Alertas.confirmar("Eliminar producto",
                "Vas a eliminar \"" + producto.getNombre() + "\".\n"
                        + "Si tiene movimientos historicos se dara de baja en lugar de borrarse.");
        if (!confirmar) {
            return;
        }
        try {
            boolean borrado = AppContexto.inventario().eliminarProducto(producto.getId());
            cargar();
            if (borrado) {
                Aplicacion.estado("Producto eliminado: " + producto.getNombre());
            } else {
                Alertas.aviso("El producto tiene movimientos de stock, por lo que se ha dado de BAJA "
                        + "en vez de borrarse (se conserva el historial).");
                Aplicacion.estado("Producto dado de baja: " + producto.getNombre());
            }
        } catch (ExcepcionNegocio e) {
            Alertas.error(e.getMessage());
        }
    }

    /** Muestra el codigo de barras y permite guardar la imagen PNG. */
    @FXML
    private void verCodigo() {
        Producto producto = seleccionado();
        if (producto == null) {
            return;
        }
        try {
            var imagen = AppContexto.codigos().generarImagen(producto.getCodigoBarras());
            ImageView vista = new ImageView(SwingFXUtils.toFXImage(imagen, null));
            vista.setPreserveRatio(true);
            vista.setFitWidth(430);

            VBox contenido = new VBox(8, vista,
                    new Label(producto.getNombre()),
                    new Label(producto.getCodigoBarras()));
            contenido.setStyle("-fx-alignment: center; -fx-padding: 16;");

            Dialog<ButtonType> dialogo = new Dialog<>();
            dialogo.setTitle("Codigo de barras");
            dialogo.setHeaderText(producto.getNombre());
            ButtonType guardar = new ButtonType("Guardar PNG", ButtonType.OK.getButtonData());
            dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CLOSE);
            dialogo.getDialogPane().setContent(contenido);

            Optional<ButtonType> respuesta = dialogo.showAndWait();
            if (respuesta.isPresent() && respuesta.get() == guardar) {
                var ruta = AppContexto.codigos().guardarPng(producto.getCodigoBarras());
                Alertas.info("Imagen guardada en:\n" + ruta.toAbsolutePath());
            }
        } catch (Exception e) {
            Alertas.excepcion("No se pudo generar el codigo de barras", e);
        }
    }

    // ================================================================== //
    //                          CARGA DE DATOS                             //
    // ================================================================== //

    private void cargar() {
        if (cargando) {
            return;
        }
        cargando = true;
        try {
            refrescar();
        } finally {
            cargando = false;
        }
    }

    private void refrescar() {
        mapaCategorias = AppContexto.inventario().mapaCategorias();
        mapaProveedores = AppContexto.inventario().mapaProveedores();

        List<Categoria> categorias = AppContexto.inventario().listarCategorias();
        Categoria seleccion = comboCategoria.getSelectionModel().getSelectedItem();
        comboCategoria.getItems().setAll(categorias);
        comboCategoria.getItems().add(0, todas);
        if (seleccion != null) {
            Optional<Categoria> igual = categorias.stream()
                    .filter(c -> seleccion.getId() != null && seleccion.getId().equals(c.getId()))
                    .findFirst();
            comboCategoria.getSelectionModel().select(igual.orElse(todas));
        } else {
            comboCategoria.getSelectionModel().selectFirst();
        }

        Categoria filtro = comboCategoria.getSelectionModel().getSelectedItem();
        Long categoriaId = (filtro == null || filtro == todas || filtro.getId() == null)
                ? null : filtro.getId();

        List<Producto> productos = AppContexto.inventario()
                .listarProductos(campoBusqueda.getText(), categoriaId, checkInactivos.isSelected());

        tabla.getItems().setAll(productos);
        etiquetaContador.setText(productos.size() + " productos");
    }

    private Producto seleccionado() {
        Producto producto = tabla.getSelectionModel().getSelectedItem();
        if (producto == null) {
            Alertas.aviso("Selecciona primero un producto de la lista.");
        }
        return producto;
    }

    // ================================================================== //
    //                     DIALOGO ALTA / EDICION                          //
    // ================================================================== //

    private Optional<Producto> pedirDatosProducto(Producto producto) {
        boolean esNuevo = producto.getId() == null;

        Dialog<Producto> dialogo = new Dialog<>();
        dialogo.setTitle(esNuevo ? "Nuevo producto" : "Editar producto");
        dialogo.setHeaderText(esNuevo ? "Alta de producto en el inventario" : producto.getNombre());
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogo.getDialogPane().setPrefWidth(520);

        TextField campoCodigo = new TextField(producto.getCodigoBarras());
        TextField campoNombre = new TextField(producto.getNombre());
        TextField campoDescripcion = new TextField(producto.getDescripcion());
        ComboBox<Categoria> comboCat = new ComboBox<>(
                javafx.collections.FXCollections.observableArrayList(
                        AppContexto.inventario().listarCategorias()));
        ComboBox<Proveedor> comboProv = new ComboBox<>(
                javafx.collections.FXCollections.observableArrayList(
                        AppContexto.inventario().listarProveedores()));
        TextField campoCompra = new TextField(num(producto.getPrecioCompra()));
        TextField campoVenta = new TextField(num(producto.getPrecioVenta()));
        TextField campoMinimo = new TextField(String.valueOf(producto.getStockMinimo()));
        TextField campoStock = new TextField(String.valueOf(producto.getStock()));
        ComboBox<String> comboUnidad = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(UNIDADES));
        comboUnidad.getSelectionModel().select(producto.getUnidad());
        CheckBox checkActivo = new CheckBox("Producto activo (visible en la venta)");
        checkActivo.setSelected(producto.isActivo());

        comboCat.getItems().stream()
                .filter(c -> c.getId() != null && c.getId().equals(producto.getCategoriaId()))
                .findFirst().ifPresent(comboCat.getSelectionModel()::select);
        comboProv.getItems().stream()
                .filter(p -> p.getId() != null && p.getId().equals(producto.getProveedorId()))
                .findFirst().ifPresent(comboProv.getSelectionModel()::select);

        comboCat.setPrefWidth(260);
        comboProv.setPrefWidth(260);

        Button botonCodigo = new Button("Generar");
        botonCodigo.setOnAction(e -> campoCodigo.setText(AppContexto.codigos().siguienteCodigo()));
        HBox cajaCodigo = new HBox(6, campoCodigo, botonCodigo);
        HBox.setHgrow(campoCodigo, Priority.ALWAYS);

        Button botonNuevaCat = new Button("+");
        botonNuevaCat.setOnAction(e -> Alertas.pedirTexto("Nueva categoria",
                "Nombre de la categoria:", "").ifPresent(nombre -> {
                    Categoria creada = new Categoria();
                    creada.setNombre(nombre);
                    try {
                        creada = AppContexto.inventario().guardarCategoria(creada);
                        comboCat.getItems().add(creada);
                        comboCat.getSelectionModel().select(creada);
                    } catch (ExcepcionNegocio ex) {
                        Alertas.error(ex.getMessage());
                    }
                }));
        HBox cajaCat = new HBox(6, comboCat, botonNuevaCat);
        HBox.setHgrow(comboCat, Priority.ALWAYS);

        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(9);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(130);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setPrefWidth(340);
        rejilla.getColumnConstraints().addAll(c1, c2);

        int fila = 0;
        fila = agregarFila(rejilla, fila, "Codigo de barras", cajaCodigo);
        fila = agregarFila(rejilla, fila, "Nombre *", campoNombre);
        fila = agregarFila(rejilla, fila, "Descripcion", campoDescripcion);
        fila = agregarFila(rejilla, fila, "Categoria", cajaCat);
        fila = agregarFila(rejilla, fila, "Proveedor", comboProv);
        fila = agregarFila(rejilla, fila, "Precio compra", campoCompra);
        fila = agregarFila(rejilla, fila, "Precio venta", campoVenta);
        fila = agregarFila(rejilla, fila, "Stock minimo", campoMinimo);
        fila = agregarFila(rejilla, fila, "Unidad", comboUnidad);
        if (esNuevo) {
            fila = agregarFila(rejilla, fila, "Stock inicial", campoStock);
        } else {
            Label aviso = new Label("El stock se modifica con entradas y salidas: "
                    + producto.getStock() + " " + producto.getUnidad());
            aviso.setStyle("-fx-text-fill:#7b8798; -fx-font-size:11px;");
            GridPane.setHalignment(aviso, javafx.geometry.HPos.RIGHT);
            rejilla.add(aviso, 1, fila++);
        }
        rejilla.add(checkActivo, 1, fila);

        VBox raiz = new VBox(rejilla);
        raiz.setSpacing(6);
        dialogo.getDialogPane().setContent(raiz);

        Producto[] resultado = new Producto[1];
        Node botonAceptar = dialogo.getDialogPane().lookupButton(ButtonType.OK);
        botonAceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            try {
                Producto construido = new Producto();
                construido.setId(producto.getId());
                construido.setFechaRegistro(producto.getFechaRegistro());

                String codigo = campoCodigo.getText().trim();
                Optional<String> errorCodigo = AppContexto.codigos().validarCodigo(codigo);
                if (errorCodigo.isPresent()) {
                    throw new ExcepcionNegocio(errorCodigo.get());
                }
                construido.setCodigoBarras(codigo);

                if (campoNombre.getText() == null || campoNombre.getText().isBlank()) {
                    throw new ExcepcionNegocio("El nombre del producto es obligatorio.");
                }
                construido.setNombre(campoNombre.getText().trim());
                construido.setDescripcion(campoDescripcion.getText());
                construido.setCategoriaId(comboCat.getValue() == null ? null : comboCat.getValue().getId());
                construido.setProveedorId(comboProv.getValue() == null ? null : comboProv.getValue().getId());
                construido.setPrecioCompra(decimal(campoCompra.getText(), "Precio de compra"));
                construido.setPrecioVenta(decimal(campoVenta.getText(), "Precio de venta"));
                construido.setStockMinimo(entero(campoMinimo.getText(), "Stock minimo"));
                construido.setUnidad(comboUnidad.getValue());
                construido.setStock(esNuevo
                        ? entero(campoStock.getText(), "Stock inicial")
                        : producto.getStock());
                construido.setActivo(checkActivo.isSelected());

                resultado[0] = construido;
            } catch (ExcepcionNegocio | NumberFormatException e) {
                Alertas.error(e.getMessage());
                evento.consume();
            }
        });

        dialogo.setResultConverter(boton -> boton == ButtonType.OK ? resultado[0] : null);
        return dialogo.showAndWait();
    }

    // ================================================================== //
    //                           AUXILIARES                                //
    // ================================================================== //

    private static int agregarFila(GridPane rejilla, int fila, String etiqueta, Node campo) {
        Label texto = new Label(etiqueta);
        texto.setStyle("-fx-font-weight:bold; -fx-text-fill:#44566c;");
        GridPane.setHalignment(texto, javafx.geometry.HPos.RIGHT);
        rejilla.add(texto, 0, fila);
        rejilla.add(campo, 1, fila);
        return fila + 1;
    }

    private static String num(BigDecimal valor) {
        return valor == null ? "0" : valor.toPlainString();
    }

    private static BigDecimal decimal(String texto, String campo) {
        String limpio = texto == null ? "" : texto.trim().replace("€", "").replace(" ", "");
        if (limpio.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(limpio.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new NumberFormatException("El campo \"" + campo + "\" no es un numero valido: " + texto);
        }
    }

    private static int entero(String texto, String campo) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(limpio);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("El campo \"" + campo + "\" debe ser un numero entero: " + texto);
        }
    }
}
