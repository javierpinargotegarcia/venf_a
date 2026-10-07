package com.inventario.servicio;

import com.inventario.excepcion.ExcepcionNegocio;
import com.inventario.modelo.Categoria;
import com.inventario.modelo.Movimiento;
import com.inventario.modelo.Producto;
import com.inventario.modelo.Proveedor;
import com.inventario.modelo.TipoMovimiento;
import com.inventario.repositorio.FactoriaRepositorios;
import com.inventario.seguridad.Sesion;
import com.inventario.util.UtilTextos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Toda la logica del inventario: CRUD de productos, categorias y
 * proveedores, y el registro de entradas/salidas de stock.
 */
public class ServicioInventario {

    // ================================================================== //
    //                           PRODUCTOS                                //
    // ================================================================== //

    /**
     * Lista productos con filtros de busqueda.
     *
     * @param texto           busca en codigo, nombre y descripcion (sin acentos ni mayusculas)
     * @param categoriaId     filtro por categoria (null = todas)
     * @param incluirInactivos si es true incluye los productos dados de baja
     */
    public List<Producto> listarProductos(String texto, Long categoriaId, boolean incluirInactivos) {
        List<Producto> base = FactoriaRepositorios.productos().listarTodos();
        return base.stream()
                .filter(p -> incluirInactivos || p.isActivo())
                .filter(p -> categoriaId == null || categoriaId.equals(p.getCategoriaId()))
                .filter(p -> texto == null || texto.isBlank()
                        || UtilTextos.contiene(p.getNombre(), texto)
                        || UtilTextos.contiene(p.getCodigoBarras(), texto)
                        || UtilTextos.contiene(p.getDescripcion(), texto))
                .toList();
    }

    public Optional<Producto> productoPorId(Long id) {
        return FactoriaRepositorios.productos().buscarPorId(id);
    }

    public Optional<Producto> productoPorCodigo(String codigo) {
        return FactoriaRepositorios.productos().buscarPorCodigoBarras(codigo);
    }

    /** Crea o actualiza un producto aplicando todas las validaciones. */
    public Producto guardarProducto(Producto p) throws ExcepcionNegocio {
        if (p.getNombre() == null || p.getNombre().isBlank()) {
            throw new ExcepcionNegocio("El nombre del producto es obligatorio.");
        }
        if (p.getCodigoBarras() == null || p.getCodigoBarras().isBlank()) {
            throw new ExcepcionNegocio("El codigo de barras es obligatorio.");
        }
        p.setNombre(p.getNombre().trim());
        p.setCodigoBarras(p.getCodigoBarras().trim());

        // Unicidad del codigo de barras
        Optional<Producto> duplicado =
                FactoriaRepositorios.productos().buscarPorCodigoBarras(p.getCodigoBarras());
        if (duplicado.isPresent() && !duplicado.get().getId().equals(p.getId())) {
            throw new ExcepcionNegocio(
                    "Ya existe el producto con codigo de barras \"" + p.getCodigoBarras() + "\".");
        }

        if (p.getPrecioCompra().signum() < 0 || p.getPrecioVenta().signum() < 0) {
            throw new ExcepcionNegocio("Los precios no pueden ser negativos.");
        }
        if (p.getStock() < 0) {
            throw new ExcepcionNegocio("El stock no puede ser negativo.");
        }
        if (p.getStockMinimo() < 0) {
            throw new ExcepcionNegocio("El stock minimo no puede ser negativo.");
        }
        if (p.getUnidad() == null || p.getUnidad().isBlank()) {
            p.setUnidad("UND");
        }
        if (p.getFechaRegistro() == null) {
            p.setFechaRegistro(LocalDateTime.now());
        }

        Producto existente = FactoriaRepositorios.productos().buscarPorId(p.getId()).orElse(null);
        if (existente == null) {
            // Producto nuevo: stock inicial fijado a mano
            p.setStock(p.getStock());
        } else if (p.getStock() != existente.getStock()) {
            // El stock se modifica unicamente a traves de movimientos.
            p.setStock(existente.getStock());
        }
        return FactoriaRepositorios.productos().guardar(p);
    }

    /**
     * Elimina un producto. Si tiene movimientos de stock historicos se da
     * de baja (activo = false) para no romper el historial; si no, se borra.
     *
     * @return true si se borro fisicamente, false si se dio de baja
     */
    public boolean eliminarProducto(Long id) throws ExcepcionNegocio {
        Producto producto = FactoriaRepositorios.productos().buscarPorId(id)
                .orElseThrow(() -> new ExcepcionNegocio("El producto ya no existe."));

        boolean tieneMovimientos = !FactoriaRepositorios.movimientos().listarPorProducto(id).isEmpty();
        if (tieneMovimientos) {
            producto.setActivo(false);
            FactoriaRepositorios.productos().guardar(producto);
            return false;
        }
        FactoriaRepositorios.productos().eliminar(id);
        return true;
    }

    public void reactivarProducto(Long id) throws ExcepcionNegocio {
        Producto producto = FactoriaRepositorios.productos().buscarPorId(id)
                .orElseThrow(() -> new ExcepcionNegocio("El producto ya no existe."));
        producto.setActivo(true);
        FactoriaRepositorios.productos().guardar(producto);
    }

    // ================================================================== //
    //                       MOVIMIENTOS DE STOCK                          //
    // ================================================================== //

    /**
     * Registra una entrada, salida o ajuste y actualiza el stock del producto.
     *
     * @param tipo      ENTRADA, SALIDA o AJUSTE
     * @param productoId producto afectado
     * @param cantidad  unidades (siempre positivas)
     * @param motivo    ejemplo "Compra a Proveedor X", "Venta mostrador"
     */
    public Movimiento registrarMovimiento(TipoMovimiento tipo, Long productoId, int cantidad, String motivo)
            throws ExcepcionNegocio {

        if (tipo == null) {
            throw new ExcepcionNegocio("Selecciona el tipo de movimiento.");
        }
        if (productoId == null) {
            throw new ExcepcionNegocio("Selecciona un producto.");
        }
        if (cantidad < 0) {
            throw new ExcepcionNegocio("La cantidad no puede ser negativa.");
        }
        if (tipo != TipoMovimiento.AJUSTE && cantidad == 0) {
            throw new ExcepcionNegocio("La cantidad debe ser mayor que cero.");
        }

        Producto producto = FactoriaRepositorios.productos().buscarPorId(productoId)
                .orElseThrow(() -> new ExcepcionNegocio("El producto ya no existe."));
        if (!producto.isActivo()) {
            throw new ExcepcionNegocio("El producto esta dado de baja y no admite movimientos.");
        }

        int anterior = producto.getStock();
        int nuevo;
        int cantidadRegistrada;
        switch (tipo) {
            case ENTRADA -> {
                nuevo = anterior + cantidad;
                cantidadRegistrada = cantidad;
            }
            case SALIDA -> {
                nuevo = anterior - cantidad;
                cantidadRegistrada = cantidad;
                if (nuevo < 0) {
                    throw new ExcepcionNegocio(
                            "Stock insuficiente: hay " + anterior + " " + producto.getUnidad()
                                    + " de \"" + producto.getNombre() + "\" y se solicitan "
                                    + cantidad + ".");
                }
            }
            case AJUSTE -> {
                // La cantidad es el stock realmente contado en el almacen.
                nuevo = cantidad;
                cantidadRegistrada = cantidad - anterior;
            }
            default -> throw new ExcepcionNegocio("Tipo de movimiento no soportado.");
        }

        producto.setStock(nuevo);
        FactoriaRepositorios.productos().guardar(producto);

        Movimiento movimiento = new Movimiento();
        movimiento.setProductoId(producto.getId());
        movimiento.setProductoNombre(producto.getNombre());
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidadRegistrada);
        movimiento.setStockAnterior(anterior);
        movimiento.setStockResultante(nuevo);
        movimiento.setMotivo(motivo == null || motivo.isBlank() ? tipo.getEtiqueta() : motivo.trim());
        if (Sesion.haySesion()) {
            movimiento.setUsuarioId(Sesion.getUsuario().getId());
            movimiento.setUsuarioNombre(Sesion.getNombreVisible());
        }
        movimiento.setFecha(LocalDateTime.now());
        return FactoriaRepositorios.movimientos().guardar(movimiento);
    }

    public List<Movimiento> listarMovimientos(String texto, TipoMovimiento tipo, Long productoId) {
        List<Movimiento> base = FactoriaRepositorios.movimientos().listarTodos();
        return base.stream()
                .filter(m -> tipo == null || m.getTipo() == tipo)
                .filter(m -> productoId == null || productoId.equals(m.getProductoId()))
                .filter(m -> texto == null || texto.isBlank()
                        || UtilTextos.contiene(m.getProductoNombre(), texto)
                        || UtilTextos.contiene(m.getMotivo(), texto)
                        || UtilTextos.contiene(m.getUsuarioNombre(), texto))
                .toList();
    }

    public List<Movimiento> movimientosEntre(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde == null ? LocalDateTime.MIN : desde.atStartOfDay();
        LocalDateTime fin = (hasta == null ? LocalDate.now() : hasta).plusDays(1).atStartOfDay();
        return FactoriaRepositorios.movimientos().listarTodos().stream()
                .filter(m -> m.getFecha() != null
                        && !m.getFecha().isBefore(inicio)
                        && m.getFecha().isBefore(fin))
                .toList();
    }

    // ================================================================== //
    //                         CATEGORIAS                                 //
    // ================================================================== //

    public List<Categoria> listarCategorias() {
        return FactoriaRepositorios.categorias().listarTodas();
    }

    public Categoria guardarCategoria(Categoria categoria) throws ExcepcionNegocio {
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            throw new ExcepcionNegocio("El nombre de la categoria es obligatorio.");
        }
        categoria.setNombre(categoria.getNombre().trim());
        Optional<Categoria> duplicada =
                FactoriaRepositorios.categorias().buscarPorNombre(categoria.getNombre());
        if (duplicada.isPresent() && !duplicada.get().getId().equals(categoria.getId())) {
            throw new ExcepcionNegocio("Ya existe la categoria \"" + categoria.getNombre() + "\".");
        }
        return FactoriaRepositorios.categorias().guardar(categoria);
    }

    /** No permite borrar una categoria con productos asignados. */
    public void eliminarCategoria(Long id) throws ExcepcionNegocio {
        boolean enUso = FactoriaRepositorios.productos().listarTodos().stream()
                .anyMatch(p -> id.equals(p.getCategoriaId()));
        if (enUso) {
            throw new ExcepcionNegocio(
                    "No se puede eliminar: hay productos asignados a esta categoria.");
        }
        if (!FactoriaRepositorios.categorias().eliminar(id)) {
            throw new ExcepcionNegocio("La categoria ya no existe.");
        }
    }

    public long contarProductosDeCategoria(Long categoriaId) {
        return FactoriaRepositorios.productos().listarTodos().stream()
                .filter(p -> categoriaId.equals(p.getCategoriaId()))
                .count();
    }

    // ================================================================== //
    //                         PROVEEDORES                                //
    // ================================================================== //

    public List<Proveedor> listarProveedores() {
        return FactoriaRepositorios.proveedores().listarTodos();
    }

    public Proveedor guardarProveedor(Proveedor proveedor) throws ExcepcionNegocio {
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            throw new ExcepcionNegocio("El nombre del proveedor es obligatorio.");
        }
        proveedor.setNombre(proveedor.getNombre().trim());
        if (proveedor.getEmail() != null && !proveedor.getEmail().isBlank()
                && !proveedor.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ExcepcionNegocio("El email no es valido.");
        }
        Optional<Proveedor> duplicado =
                FactoriaRepositorios.proveedores().buscarPorNombre(proveedor.getNombre());
        if (duplicado.isPresent() && !duplicado.get().getId().equals(proveedor.getId())) {
            throw new ExcepcionNegocio("Ya existe el proveedor \"" + proveedor.getNombre() + "\".");
        }
        return FactoriaRepositorios.proveedores().guardar(proveedor);
    }

    public void eliminarProveedor(Long id) throws ExcepcionNegocio {
        boolean enUso = FactoriaRepositorios.productos().listarTodos().stream()
                .anyMatch(p -> id.equals(p.getProveedorId()));
        if (enUso) {
            throw new ExcepcionNegocio(
                    "No se puede eliminar: hay productos de este proveedor.");
        }
        if (!FactoriaRepositorios.proveedores().eliminar(id)) {
            throw new ExcepcionNegocio("El proveedor ya no existe.");
        }
    }

    public long contarProductosDeProveedor(Long proveedorId) {
        return FactoriaRepositorios.productos().listarTodos().stream()
                .filter(p -> proveedorId.equals(p.getProveedorId()))
                .count();
    }

    // ================================================================== //
    //                    CONSULTAS Y RESUMENES                           //
    // ================================================================== //

    public Map<Long, Categoria> mapaCategorias() {
        return listarCategorias().stream()
                .collect(Collectors.toMap(Categoria::getId, Function.identity(), (a, b) -> a));
    }

    public Map<Long, Proveedor> mapaProveedores() {
        return listarProveedores().stream()
                .collect(Collectors.toMap(Proveedor::getId, Function.identity(), (a, b) -> a));
    }

    public String nombreCategoria(Long categoriaId, Map<Long, Categoria> mapa) {
        if (categoriaId == null) return "";
        Categoria c = mapa.get(categoriaId);
        return c == null ? "" : c.getNombre();
    }

    public String nombreProveedor(Long proveedorId, Map<Long, Proveedor> mapa) {
        if (proveedorId == null) return "";
        Proveedor p = mapa.get(proveedorId);
        return p == null ? "" : p.getNombre();
    }

    /** Productos con stock por debajo (o igual) del minimo configurado. */
    public List<Producto> productosConStockBajo() {
        return FactoriaRepositorios.productos().listarTodos().stream()
                .filter(Producto::isActivo)
                .filter(Producto::isStockBajo)
                .toList();
    }

    public ResumenInventario resumen() {
        List<Producto> activos = FactoriaRepositorios.productos().listarTodos().stream()
                .filter(Producto::isActivo)
                .toList();

        BigDecimal valor = activos.stream()
                .map(Producto::getValorStock)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int unidades = activos.stream().mapToInt(Producto::getStock).sum();
        int stockBajo = (int) activos.stream().filter(Producto::isStockBajo).count();

        LocalDate hoy = LocalDate.now();
        int movimientosHoy = (int) FactoriaRepositorios.movimientos().listarTodos().stream()
                .filter(m -> m.getFecha() != null && m.getFecha().toLocalDate().equals(hoy))
                .count();

        return new ResumenInventario(
                (int) activos.size(),
                (int) FactoriaRepositorios.productos().listarTodos().size(),
                valor,
                unidades,
                stockBajo,
                movimientosHoy);
    }

    /** Cifras generales del inventario para el panel de resumen. */
    public record ResumenInventario(int productosActivos,
                                    int productosTotales,
                                    BigDecimal valorInventario,
                                    int unidadesTotales,
                                    int productosStockBajo,
                                    int movimientosHoy) {
    }
}
