package com.inventario.servicio;

import com.inventario.AppContexto;
import com.inventario.modelo.Categoria;
import com.inventario.modelo.Movimiento;
import com.inventario.modelo.Producto;
import com.inventario.modelo.Proveedor;
import com.inventario.modelo.TipoMovimiento;
import com.inventario.repositorio.FactoriaRepositorios;
import com.inventario.seguridad.Sesion;
import com.inventario.util.UtilFechas;
import com.inventario.util.UtilTextos;
import java.awt.Color;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Generacion de los informes del inventario en <b>PDF</b> (OpenPDF)
 * y en <b>Excel</b> (Apache POI):
 *
 * <ul>
 *   <li>Inventario completo (valorizado)</li>
 *   <li>Movimientos de stock por fechas</li>
 *   <li>Productos con stock bajo (reponer)</li>
 *   <li>Etiquetas imprimibles con codigo de barras</li>
 * </ul>
 */
public class ServicioReportes {

    // Colores corporativos
    private static final Color COLOR_CABECERA = new Color(31, 73, 125);
    private static final Color COLOR_FILA_PAR = new Color(240, 244, 248);
    private static final Color COLOR_ALERTA = new Color(253, 236, 236);
    private static final Color COLOR_TOTAL = new Color(226, 234, 244);

    private static final byte[] AZUL_EXCEL = {(byte) 31, (byte) 73, (byte) 125};

    // ================================================================== //
    //                            UTILIDADES                               //
    // ================================================================== //

    private File nuevoArchivo(Path carpeta, String prefijo, String extension) throws Exception {
        Files.createDirectories(carpeta);
        return carpeta.resolve(UtilFechas.sello(prefijo, extension)).toFile();
    }

    private List<Producto> productosActivos() {
        return FactoriaRepositorios.productos().listarTodos().stream()
                .filter(Producto::isActivo)
                .toList();
    }

    private String pie() {
        String modo = AppContexto.config().esModoMysql() ? "MySQL" : "JSON (" + AppContexto.config().getCarpetaDatos() + ")";
        return "Generado: " + UtilFechas.formatear(LocalDateTime.now())
                + "  |  Usuario: " + Sesion.getNombreVisible()
                + "  |  Almacenamiento: " + modo;
    }

    // ================================================================== //
    //                                PDF                                  //
    // ================================================================== //

    /** Inventario valorizado completo (horizontal). */
    public File pdfInventario(java.nio.file.Path carpeta) throws Exception {
        List<Producto> productos = productosActivos();
        Map<Long, Categoria> cats = FactoriaRepositorios.categorias().listarTodas().stream()
                .collect(java.util.stream.Collectors.toMap(Categoria::getId, c -> c, (a, b) -> a));
        Map<Long, Proveedor> provs = FactoriaRepositorios.proveedores().listarTodos().stream()
                .collect(java.util.stream.Collectors.toMap(Proveedor::getId, p -> p, (a, b) -> a));

        File archivo = nuevoArchivo(carpeta, "Inventario", "pdf");

        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 35, 35);
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            PdfWriter.getInstance(doc, salida);
            doc.open();

            titulo(doc, "INVENTARIO DE MERCADERIA", productos.size() + " productos activos");
            doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6f)));

            float[] anchos = {15f, 21f, 13f, 15f, 6f, 6f, 8f, 8f, 8f};
            PdfPTable tabla = new PdfPTable(anchos);
            tabla.setWidthPercentage(100f);
            cabecera(tabla, "Codigo", "Producto", "Categoria", "Proveedor", "Stock", "Min.",
                    "P.Compra", "P.Venta", "Valor stock");

            BigDecimal totalValor = BigDecimal.ZERO;
            int totalUnidades = 0;
            boolean par = true;
            for (Producto p : productos) {
                Color fondo = par ? COLOR_FILA_PAR : null;
                par = !par;
                Color alerta = p.isStockBajo() ? COLOR_ALERTA : fondo;

                celda(tabla, p.getCodigoBarras(), false, alerta, Element.ALIGN_LEFT);
                celda(tabla, p.getNombre() + (p.getDescripcion() == null || p.getDescripcion().isBlank()
                        ? "" : "\n" + UtilTextos.recortar(p.getDescripcion(), 70)), false, alerta, Element.ALIGN_LEFT);
                celda(tabla, nombreCategoria(p.getCategoriaId(), cats), false, alerta, Element.ALIGN_LEFT);
                celda(tabla, nombreProveedor(p.getProveedorId(), provs), false, alerta, Element.ALIGN_LEFT);
                celda(tabla, p.getStock() + " " + p.getUnidad(), false, alerta, Element.ALIGN_RIGHT);
                celda(tabla, String.valueOf(p.getStockMinimo()), false, alerta, Element.ALIGN_RIGHT);
                celda(tabla, UtilTextos.dinero(p.getPrecioCompra()), false, alerta, Element.ALIGN_RIGHT);
                celda(tabla, UtilTextos.dinero(p.getPrecioVenta()), false, alerta, Element.ALIGN_RIGHT);
                celda(tabla, UtilTextos.dinero(p.getValorStock()), false, alerta, Element.ALIGN_RIGHT);

                totalValor = totalValor.add(p.getValorStock());
                totalUnidades += p.getStock();
            }

            // Fila de totales
            celda(tabla, "TOTALES", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            celda(tabla, totalUnidades + " UND", true, COLOR_TOTAL, Element.ALIGN_RIGHT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_RIGHT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_RIGHT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_RIGHT);
            celda(tabla, UtilTextos.moneda(totalValor), true, COLOR_TOTAL, Element.ALIGN_RIGHT);

            doc.add(tabla);

            int bajos = (int) productos.stream().filter(Producto::isStockBajo).count();
            Paragraph nota = new Paragraph(
                    "Valor total del inventario: " + UtilTextos.moneda(totalValor)
                            + "   |   Unidades: " + totalUnidades
                            + "   |   Productos con stock bajo: " + bajos,
                    fuente(9f, false, Color.DARK_GRAY));
            nota.setSpacingBefore(8f);
            doc.add(nota);

            doc.close();
        }
        return archivo;
    }

    /** Movimientos de stock entre dos fechas (inclusive). */
    public File pdfMovimientos(java.nio.file.Path carpeta, LocalDate desde, LocalDate hasta) throws Exception {        List<Movimiento> movimientos = AppContexto.inventario().movimientosEntre(desde, hasta);
        File archivo = nuevoArchivo(carpeta, "Movimientos", "pdf");

        String rango = (desde == null ? "inicio" : UtilFechas.FECHA.format(desde))
                + " a " + (hasta == null ? "hoy" : UtilFechas.FECHA.format(hasta));

        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 35, 35);
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            PdfWriter.getInstance(doc, salida);
            doc.open();

            titulo(doc, "MOVIMIENTOS DE STOCK", rango + "  |  " + movimientos.size() + " registros");
            doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6f)));

            float[] anchos = {13f, 8f, 20f, 6f, 7f, 8f, 24f, 14f};
            PdfPTable tabla = new PdfPTable(anchos);
            tabla.setWidthPercentage(100f);
            cabecera(tabla, "Fecha", "Tipo", "Producto", "Cant.", "Stock ant.", "Stock nuevo",
                    "Motivo", "Usuario");

            BigDecimal entradas = BigDecimal.ZERO;
            BigDecimal salidas = BigDecimal.ZERO;
            boolean par = true;
            for (Movimiento m : movimientos) {
                Color fondo = par ? COLOR_FILA_PAR : null;
                par = !par;
                Color tipo = m.getTipo() == TipoMovimiento.SALIDA ? COLOR_ALERTA : fondo;

                celda(tabla, UtilFechas.formatear(m.getFecha()), false, tipo, Element.ALIGN_LEFT);
                celda(tabla, m.getTipo() == null ? "" : m.getTipo().getEtiqueta(), false, tipo, Element.ALIGN_LEFT);
                celda(tabla, m.getProductoNombre(), false, tipo, Element.ALIGN_LEFT);
                celda(tabla, String.valueOf(m.getCantidad()), false, tipo, Element.ALIGN_RIGHT);
                celda(tabla, String.valueOf(m.getStockAnterior()), false, tipo, Element.ALIGN_RIGHT);
                celda(tabla, String.valueOf(m.getStockResultante()), false, tipo, Element.ALIGN_RIGHT);
                celda(tabla, m.getMotivo() == null ? "" : m.getMotivo(), false, tipo, Element.ALIGN_LEFT);
                celda(tabla, m.getUsuarioNombre() == null ? "" : m.getUsuarioNombre(), false, tipo, Element.ALIGN_LEFT);
            }

            celda(tabla, "TOTALES", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            celda(tabla, "", true, COLOR_TOTAL, Element.ALIGN_LEFT);
            doc.add(tabla);

            for (Movimiento m : movimientos) {
                if (m.getTipo() == TipoMovimiento.ENTRADA) {
                    entradas = entradas.add(BigDecimal.valueOf(m.getCantidad()));
                } else if (m.getTipo() == TipoMovimiento.SALIDA) {
                    salidas = salidas.add(BigDecimal.valueOf(m.getCantidad()));
                }
            }
            Paragraph nota = new Paragraph(
                    "Entradas: " + entradas.toBigInteger() + " uds.  |  Salidas: " + salidas.toBigInteger()
                            + " uds.  |  Balance: " + entradas.subtract(salidas).toBigInteger() + " uds.",
                    fuente(9f, false, Color.DARK_GRAY));
            nota.setSpacingBefore(8f);
            doc.add(nota);

            doc.close();
        }
        return archivo;
    }

    /** Productos por debajo del stock minimo, con las unidades a reponer. */
    public File pdfStockBajo(java.nio.file.Path carpeta) throws Exception {
        List<Producto> bajos = AppContexto.inventario().productosConStockBajo();
        File archivo = nuevoArchivo(carpeta, "Stock_bajo", "pdf");

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            PdfWriter.getInstance(doc, salida);
            doc.open();

            titulo(doc, "PRODUCTOS A REponer".toUpperCase(), bajos.size() + " productos con stock bajo");
            doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6f)));

            float[] anchos = {22f, 34f, 14f, 10f, 10f, 10f};
            PdfPTable tabla = new PdfPTable(anchos);
            tabla.setWidthPercentage(100f);
            cabecera(tabla, "Codigo", "Producto", "Categoria", "Stock", "Minimo", "A reponer");

            for (Producto p : bajos) {
                int sugerido = Math.max(p.getStockMinimo() * 2 - p.getStock(), 1);
                celda(tabla, p.getCodigoBarras(), false, COLOR_ALERTA, Element.ALIGN_LEFT);
                celda(tabla, p.getNombre(), false, COLOR_ALERTA, Element.ALIGN_LEFT);
                celda(tabla, "", false, COLOR_ALERTA, Element.ALIGN_LEFT);
                celda(tabla, String.valueOf(p.getStock()), false, COLOR_ALERTA, Element.ALIGN_RIGHT);
                celda(tabla, String.valueOf(p.getStockMinimo()), false, COLOR_ALERTA, Element.ALIGN_RIGHT);
                celda(tabla, String.valueOf(sugerido), true, COLOR_ALERTA, Element.ALIGN_RIGHT);
            }
            if (bajos.isEmpty()) {
                celda(tabla, "No hay productos con stock bajo. Inventario al dia.", false, null, Element.ALIGN_LEFT);
                for (int i = 0; i < 5; i++) {
                    celda(tabla, "", false, null, Element.ALIGN_LEFT);
                }
            }
            doc.add(tabla);
            doc.close();
        }
        return archivo;
    }

    /** Hoja de etiquetas imprimibles con el codigo de barras de cada producto. */
    public File pdfEtiquetas(java.nio.file.Path carpeta) throws Exception {
        List<Producto> productos = productosActivos();
        ServicioCodigoBarras codigos = AppContexto.codigos();
        File archivo = nuevoArchivo(carpeta, "Etiquetas_codigos_barras", "pdf");

        Document doc = new Document(PageSize.A4, 25, 25, 30, 30);
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            PdfWriter.getInstance(doc, salida);
            doc.open();

            titulo(doc, "ETIQUETAS CON CODIGO DE BARRAS", productos.size() + " etiquetas");
            doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6f)));

            PdfPTable tabla = new PdfPTable(3);
            tabla.setWidthPercentage(100f);

            for (Producto p : productos) {
                if (p.getCodigoBarras() == null || p.getCodigoBarras().isBlank()) {
                    continue;
                }
                PdfPCell celda = new PdfPCell();
                celda.setBorder(PdfPCell.BOX);
                celda.setBorderColor(Color.LIGHT_GRAY);
                celda.setPadding(6f);
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);

                try {
                    Image imagen = Image.getInstance(codigos.generarPng(p.getCodigoBarras()));
                    imagen.scaleAbsolute(105f, 40f);
                    imagen.setAlignment(Image.ALIGN_CENTER);
                    celda.addElement(imagen);
                } catch (Exception e) {
                    celda.addElement(new Phrase("(sin imagen)", fuente(7f, false, Color.RED)));
                }

                Paragraph nombre = new Paragraph(UtilTextos.recortar(p.getNombre(), 28),
                        fuente(8f, true, Color.BLACK));
                nombre.setAlignment(Element.ALIGN_CENTER);
                nombre.setSpacingBefore(2f);
                celda.addElement(nombre);

                Paragraph codigo = new Paragraph(p.getCodigoBarras(), fuente(8f, false, Color.DARK_GRAY));
                codigo.setAlignment(Element.ALIGN_CENTER);
                celda.addElement(codigo);

                Paragraph precio = new Paragraph(UtilTextos.moneda(p.getPrecioVenta()),
                        fuente(9f, true, COLOR_CABECERA));
                precio.setAlignment(Element.ALIGN_CENTER);
                celda.addElement(precio);

                tabla.addCell(celda);
            }
            doc.add(tabla);
            doc.close();
        }
        return archivo;
    }

    // ================================================================== //
    //                               EXCEL                                 //
    // ================================================================== //

    /** Inventario valorizado en formato Excel. */
    public File excelInventario(java.nio.file.Path carpeta) throws Exception {
        List<Producto> productos = productosActivos();
        Map<Long, Categoria> cats = FactoriaRepositorios.categorias().listarTodas().stream()
                .collect(java.util.stream.Collectors.toMap(Categoria::getId, c -> c, (a, b) -> a));
        Map<Long, Proveedor> provs = FactoriaRepositorios.proveedores().listarTodos().stream()
                .collect(java.util.stream.Collectors.toMap(Proveedor::getId, p -> p, (a, b) -> a));

        File archivo = nuevoArchivo(carpeta, "Inventario", "xlsx");

        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            XSSFSheet hoja = libro.createSheet("Inventario");
            CellStyle[] estilos = crearEstilos(libro);
            int fila = cabeceraExcel(hoja, estilos, "INVENTARIO DE MERCADERIA",
                    productos.size() + " productos activos");

            String[] columnas = {"Codigo", "Producto", "Descripcion", "Categoria", "Proveedor",
                    "Unidad", "Stock", "Stock min.", "P. compra", "P. venta", "Valor stock", "Estado"};
            fila = filaCabeceraExcel(hoja, estilos, fila, columnas);

            BigDecimal total = BigDecimal.ZERO;
            for (Producto p : productos) {
                XSSFRow f = hoja.createRow(fila++);
                ponerTexto(f, 0, p.getCodigoBarras(), estilos[0]);
                ponerTexto(f, 1, p.getNombre(), estilos[0]);
                ponerTexto(f, 2, p.getDescripcion(), estilos[0]);
                ponerTexto(f, 3, nombreCategoria(p.getCategoriaId(), cats), estilos[0]);
                ponerTexto(f, 4, nombreProveedor(p.getProveedorId(), provs), estilos[0]);
                ponerTexto(f, 5, p.getUnidad(), estilos[0]);
                ponerNumero(f, 6, p.getStock(), estilos[2]);
                ponerNumero(f, 7, p.getStockMinimo(), estilos[2]);
                ponerNumero(f, 8, p.getPrecioCompra().doubleValue(), estilos[1]);
                ponerNumero(f, 9, p.getPrecioVenta().doubleValue(), estilos[1]);
                ponerNumero(f, 10, p.getValorStock().doubleValue(), estilos[1]);
                ponerTexto(f, 11, p.isStockBajo() ? "STOCK BAJO" : "OK", estilos[0]);
                total = total.add(p.getValorStock());
            }

            XSSFRow totalRow = hoja.createRow(fila);
            XSSFCell celdaTotal = totalRow.createCell(0);
            celdaTotal.setCellValue("TOTAL");
            celdaTotal.setCellStyle(estilos[3]);
            for (int i = 1; i < 10; i++) {
                totalRow.createCell(i).setCellStyle(estilos[3]);
            }
            XSSFCell importe = totalRow.createCell(10);
            importe.setCellValue(total.doubleValue());
            importe.setCellStyle(estilos[4]);
            totalRow.createCell(11).setCellStyle(estilos[3]);

            autoAncho(hoja, columnas.length);
            try (FileOutputStream salida = new FileOutputStream(archivo)) {
                libro.write(salida);
            }
        }
        return archivo;
    }

    /** Movimientos de stock entre dos fechas en formato Excel. */
    public File excelMovimientos(java.nio.file.Path carpeta, LocalDate desde, LocalDate hasta) throws Exception {
        List<Movimiento> movimientos = AppContexto.inventario().movimientosEntre(desde, hasta);
        File archivo = nuevoArchivo(carpeta, "Movimientos", "xlsx");

        String rango = (desde == null ? "inicio" : UtilFechas.FECHA.format(desde))
                + " a " + (hasta == null ? "hoy" : UtilFechas.FECHA.format(hasta));

        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            XSSFSheet hoja = libro.createSheet("Movimientos");
            CellStyle[] estilos = crearEstilos(libro);
            int fila = cabeceraExcel(hoja, estilos, "MOVIMIENTOS DE STOCK",
                    rango + "  |  " + movimientos.size() + " registros");

            String[] columnas = {"Fecha", "Tipo", "Producto", "Cantidad", "Stock anterior",
                    "Stock resultante", "Motivo", "Usuario"};
            fila = filaCabeceraExcel(hoja, estilos, fila, columnas);

            for (Movimiento m : movimientos) {
                XSSFRow f = hoja.createRow(fila++);
                ponerTexto(f, 0, UtilFechas.formatear(m.getFecha()), estilos[0]);
                ponerTexto(f, 1, m.getTipo() == null ? "" : m.getTipo().getEtiqueta(), estilos[0]);
                ponerTexto(f, 2, m.getProductoNombre(), estilos[0]);
                ponerNumero(f, 3, m.getCantidad(), estilos[2]);
                ponerNumero(f, 4, m.getStockAnterior(), estilos[2]);
                ponerNumero(f, 5, m.getStockResultante(), estilos[2]);
                ponerTexto(f, 6, m.getMotivo(), estilos[0]);
                ponerTexto(f, 7, m.getUsuarioNombre(), estilos[0]);
            }

            autoAncho(hoja, columnas.length);
            try (FileOutputStream salida = new FileOutputStream(archivo)) {
                libro.write(salida);
            }
        }
        return archivo;
    }

    /** Productos a reponer en formato Excel. */
    public File excelStockBajo(java.nio.file.Path carpeta) throws Exception {
        List<Producto> bajos = AppContexto.inventario().productosConStockBajo();
        File archivo = nuevoArchivo(carpeta, "Stock_bajo", "xlsx");

        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            XSSFSheet hoja = libro.createSheet("Stock bajo");
            CellStyle[] estilos = crearEstilos(libro);
            int fila = cabeceraExcel(hoja, estilos, "PRODUCTOS A REPONER",
                    bajos.size() + " productos con stock bajo");

            String[] columnas = {"Codigo", "Producto", "Unidad", "Stock", "Stock minimo", "A reponer"};
            fila = filaCabeceraExcel(hoja, estilos, fila, columnas);

            for (Producto p : bajos) {
                int sugerido = Math.max(p.getStockMinimo() * 2 - p.getStock(), 1);
                XSSFRow f = hoja.createRow(fila++);
                ponerTexto(f, 0, p.getCodigoBarras(), estilos[0]);
                ponerTexto(f, 1, p.getNombre(), estilos[0]);
                ponerTexto(f, 2, p.getUnidad(), estilos[0]);
                ponerNumero(f, 3, p.getStock(), estilos[2]);
                ponerNumero(f, 4, p.getStockMinimo(), estilos[2]);
                ponerNumero(f, 5, sugerido, estilos[2]);
            }

            autoAncho(hoja, columnas.length);
            try (FileOutputStream salida = new FileOutputStream(archivo)) {
                libro.write(salida);
            }
        }
        return archivo;
    }

    // ================================================================== //
    //                       AUXILIARES DE PDF                            //
    // ================================================================== //

    private void titulo(Document doc, String texto, String subtitulo) throws Exception {
        Paragraph titulo = new Paragraph(texto, fuente(16f, true, COLOR_CABECERA));
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph sub = new Paragraph(subtitulo, fuente(10f, false, Color.DARK_GRAY));
        sub.setAlignment(Element.ALIGN_CENTER);
        doc.add(sub);

        Paragraph piePagina = new Paragraph(pie(), fuente(7f, false, Color.GRAY));
        piePagina.setAlignment(Element.ALIGN_CENTER);
        piePagina.setSpacingAfter(6f);
        doc.add(piePagina);

        // Barra separadora
        PdfPTable barra = new PdfPTable(1);
        barra.setWidthPercentage(100f);
        PdfPCell celdaBarra = new PdfPCell(new Phrase(" ", fuente(2f, false, COLOR_CABECERA)));
        celdaBarra.setBackgroundColor(COLOR_CABECERA);
        celdaBarra.setPadding(0f);
        barra.addCell(celdaBarra);
        doc.add(barra);
        doc.add(new Paragraph(" ", fuente(6f, false, Color.WHITE)));
    }

    private void cabecera(PdfPTable tabla, String... columnas) {
        for (String columna : columnas) {
            PdfPCell celda = new PdfPCell(new Phrase(columna, fuente(8f, true, Color.WHITE)));
            celda.setBackgroundColor(COLOR_CABECERA);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
            celda.setPadding(5f);
            tabla.addCell(celda);
        }
    }

    private void celda(PdfPTable tabla, String texto, boolean negrita, Color fondo, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "" : texto,
                fuente(7f, negrita, Color.BLACK)));
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(4f);
        if (fondo != null) {
            celda.setBackgroundColor(fondo);
        }
        tabla.addCell(celda);
    }

    private static Font fuente(float tamano, boolean negrita, Color color) {
        return FontFactory.getFont(negrita ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA,
                tamano, color);
    }

    private String nombreCategoria(Long id, Map<Long, Categoria> mapa) {
        if (id == null) return "";
        Categoria c = mapa.get(id);
        return c == null ? "" : c.getNombre();
    }

    private String nombreProveedor(Long id, Map<Long, Proveedor> mapa) {
        if (id == null) return "";
        Proveedor p = mapa.get(id);
        return p == null ? "" : p.getNombre();
    }

    // ================================================================== //
    //                      AUXILIARES DE EXCEL                           //
    // ================================================================== //

    /**
     * Estilos comunes del libro.
     * indice 0 = texto, 1 = moneda, 2 = entero, 3 = cabecera de totales, 4 = moneda total.
     */
    private CellStyle[] crearEstilos(XSSFWorkbook libro) {
        CellStyle[] estilos = new CellStyle[5];

        CellStyle texto = libro.createCellStyle();
        org.apache.poi.ss.usermodel.Font fTexto = libro.createFont();
        fTexto.setFontHeightInPoints((short) 10);
        texto.setFont(fTexto);
        texto.setVerticalAlignment(VerticalAlignment.CENTER);
        texto.setBorderBottom(BorderStyle.THIN);
        estilos[0] = texto;

        CellStyle moneda = libro.createCellStyle();
        moneda.cloneStyleFrom(texto);
        moneda.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));
        moneda.setAlignment(HorizontalAlignment.RIGHT);
        estilos[1] = moneda;

        CellStyle entero = libro.createCellStyle();
        entero.cloneStyleFrom(texto);
        entero.setDataFormat(libro.createDataFormat().getFormat("#,##0"));
        entero.setAlignment(HorizontalAlignment.RIGHT);
        estilos[2] = entero;

        org.apache.poi.xssf.usermodel.XSSFCellStyle totales = libro.createCellStyle();
        totales.cloneStyleFrom(texto);
        org.apache.poi.ss.usermodel.Font fTotal = libro.createFont();
        fTotal.setBold(true);
        fTotal.setColor(IndexedColors.WHITE.getIndex());
        totales.setFont(fTotal);
        totales.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(AZUL_EXCEL, null));
        totales.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilos[3] = totales;

        CellStyle monedaTotal = libro.createCellStyle();
        monedaTotal.cloneStyleFrom(totales);
        monedaTotal.setFont(fTotal);
        monedaTotal.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));
        monedaTotal.setAlignment(HorizontalAlignment.RIGHT);
        estilos[4] = monedaTotal;

        return estilos;
    }

    /** Escribe el bloque de titulo y devuelve la siguiente fila libre. */
    private int cabeceraExcel(XSSFSheet hoja, CellStyle[] estilos, String titulo, String subtitulo) {
        XSSFRow filaTitulo = hoja.createRow(0);
        XSSFCell celdaTitulo = filaTitulo.createCell(0);
        celdaTitulo.setCellValue(titulo);
        org.apache.poi.ss.usermodel.Font fuenteTitulo = hoja.getWorkbook().createFont();
        fuenteTitulo.setBold(true);
        fuenteTitulo.setFontHeightInPoints((short) 14);
        CellStyle estiloTitulo = hoja.getWorkbook().createCellStyle();
        estiloTitulo.setFont(fuenteTitulo);
        celdaTitulo.setCellStyle(estiloTitulo);

        hoja.createRow(1).createCell(0).setCellValue(subtitulo);
        hoja.createRow(2).createCell(0).setCellValue(pie());
        return 4;
    }

    private int filaCabeceraExcel(XSSFSheet hoja, CellStyle[] estilos, int indice, String[] columnas) {
        XSSFRow fila = hoja.createRow(indice);
        for (int i = 0; i < columnas.length; i++) {
            XSSFCell celda = fila.createCell(i);
            celda.setCellValue(columnas[i]);
            celda.setCellStyle(estilos[3]);
        }
        return indice + 1;
    }

    private void ponerTexto(XSSFRow fila, int columna, String valor, CellStyle estilo) {
        XSSFCell celda = fila.createCell(columna);
        celda.setCellValue(valor == null ? "" : valor);
        celda.setCellStyle(estilo);
    }

    private void ponerNumero(XSSFRow fila, int columna, double valor, CellStyle estilo) {
        XSSFCell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private void autoAncho(XSSFSheet hoja, int columnas) {
        try {
            for (int i = 0; i < columnas; i++) {
                hoja.autoSizeColumn(i);
            }
        } catch (Throwable ignorado) {
            // El autoajuste necesita AWT; si no esta disponible se usan anchos fijos.
            for (int i = 0; i < columnas; i++) {
                hoja.setColumnWidth(i, 18 * 256);
            }
        }
    }

    /** Ruta por defecto donde se guardan los informes: {@code data/informes}. */
    public java.nio.file.Path carpetaInformes() {
        return AppContexto.config().getDirectorioDatos().resolve("informes");
    }

    /** Ruta por defecto donde se guardan las etiquetas: {@code data/informes}. */
    public java.nio.file.Path carpetaEtiquetas() {
        return carpetaInformes();
    }

    /** Lista los informes generados previamente. */
    public List<File> informesGenerados() {
        List<File> archivos = new ArrayList<>();
        java.nio.file.Path carpeta = carpetaInformes();
        if (java.nio.file.Files.isDirectory(carpeta)) {
            try (var flujo = java.nio.file.Files.list(carpeta)) {
                flujo.filter(p -> p.toString().toLowerCase().endsWith(".pdf")
                                || p.toString().toLowerCase().endsWith(".xlsx"))
                        .sorted(java.util.Comparator.comparing(
                                (java.nio.file.Path p) -> p).reversed())
                        .forEach(p -> archivos.add(p.toFile()));
            } catch (Exception e) {
                // nada que hacer
            }
        }
        return archivos;
    }
}
