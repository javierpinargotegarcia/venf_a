package com.inventario.datos;

import com.inventario.modelo.Categoria;
import com.inventario.modelo.Movimiento;
import com.inventario.modelo.Producto;
import com.inventario.modelo.Proveedor;
import com.inventario.modelo.RolUsuario;
import com.inventario.modelo.TipoMovimiento;
import com.inventario.modelo.Usuario;
import com.inventario.repositorio.FactoriaRepositorios;
import com.inventario.servicio.ServicioCodigoBarras;
import com.inventario.util.UtilHash;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de ejemplo que se cargan la primera vez que se arranca la
 * aplicacion en modo JSON (repositorio vacio). Incluye el usuario
 * administrador por defecto.
 */
public final class DatosSemilla {

    public static final String USUARIO_ADMIN = "admin";
    public static final String CLAVE_ADMIN = "admin123";
    public static final String USUARIO_OPERARIO = "operario";
    public static final String CLAVE_OPERARIO = "operario123";

    private DatosSemilla() {
    }

    /** Carga los datos de ejemplo solo si el repositorio esta vacio. */
    public static synchronized void sembrarSiVacio() {
        boolean algoCargado = false;

        if (FactoriaRepositorios.usuariosJson().listarTodos().isEmpty()) {
            sembrarUsuarios();
            algoCargado = true;
        }
        if (FactoriaRepositorios.categoriasJson().listarTodas().isEmpty()) {
            sembrarCategorias();
            algoCargado = true;
        }
        if (FactoriaRepositorios.proveedoresJson().listarTodos().isEmpty()) {
            sembrarProveedores();
            algoCargado = true;
        }
        if (FactoriaRepositorios.productosJson().listarTodos().isEmpty()) {
            sembrarProductos();
            algoCargado = true;
        }

        if (algoCargado) {
            System.out.println("[inventario] Repositorio nuevo: datos de ejemplo cargados en "
                    + com.inventario.AppContexto.config().getDirectorioDatos() + "/");
        }
    }

    // ------------------------------------------------------------------ //

    private static void sembrarUsuarios() {
        crearUsuario(1L, USUARIO_ADMIN, CLAVE_ADMIN, "Administrador del Sistema", RolUsuario.ADMIN);
        crearUsuario(2L, USUARIO_OPERARIO, CLAVE_OPERARIO, "Operario de Almacen", RolUsuario.OPERARIO);
    }

    private static void crearUsuario(long id, String usuario, String clave, String nombre, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsuario(usuario);
        u.setPasswordHash(UtilHash.crear(clave));
        u.setNombreCompleto(nombre);
        u.setRol(rol);
        u.setActivo(true);
        u.setFechaRegistro(LocalDateTime.now().minusDays(30));
        FactoriaRepositorios.usuariosJson().guardar(u);
    }

    private static void sembrarCategorias() {
        String[][] datos = {
                {"Bebidas", "Aguas, refrescos, zumos y cafe"},
                {"Snacks", "Aperitivos, galletas y golosinas"},
                {"Lacteos", "Leche, yogures, queso y mantequilla"},
                {"Abarrotes", "Productos secos, conservas y aceites"},
                {"Limpieza", "Detergentes y productos de higiene del hogar"},
                {"Conservas", "Latas de pescado y vegetales"}
        };
        long id = 1;
        for (String[] d : datos) {
            Categoria c = new Categoria(id++, d[0], d[1]);
            FactoriaRepositorios.categoriasJson().guardar(c);
        }
    }

    private static void sembrarProveedores() {
        Object[][] datos = {
                {"Distribuciones La Norte S.A.", "Marta Ferrer", "+34 911 22 33 44",
                        "ventas@lanorte.es", "Poligono Industrial Norte, nave 12"},
                {"Alimentos del Valle S.L.", "Jorge Campos", "+34 933 55 66 77",
                        "pedidos@alimentosvalle.es", "Camino de la Vega 45"},
                {"Limpiezas Integrales Ibérica", "Sonia Prat", "+34 965 88 99 00",
                        "comercial@limpiezasiberica.es", "Avda. del Puerto 203"}
        };
        long id = 1;
        for (Object[] d : datos) {
            Proveedor p = new Proveedor();
            p.setId(id++);
            p.setNombre((String) d[0]);
            p.setContacto((String) d[1]);
            p.setTelefono((String) d[2]);
            p.setEmail((String) d[3]);
            p.setDireccion((String) d[4]);
            FactoriaRepositorios.proveedoresJson().guardar(p);
        }
    }

    private static void sembrarProductos() {
        // id, nombre, categoriaId, proveedorId, pc, pv, stock, stockMinimo, unidad, descripcion
        Object[][] datos = {
                {"Agua mineral 1,5 L", 1L, 1L, "0.45", "0.75", 120, 20, "UND", "Agua mineral natural de manantial"},
                {"Refresco de cola 2 L", 1L, 1L, "1.10", "1.85", 80, 15, "UND", "Refresco de cola, botella de 2 litros"},
                {"Cafe molido 250 g", 4L, 2L, "2.30", "3.95", 45, 10, "UND", "Cafe molido torrefaccion natural"},
                {"Galletas de chocolate 200 g", 2L, 2L, "1.20", "2.10", 60, 12, "UND", "Galletas rellenas de chocolate negro"},
                {"Patatas fritas 150 g", 2L, 2L, "0.95", "1.65", 90, 20, "UND", "Patatas fritas con sal"},
                {"Leche entera 1 L", 3L, 2L, "0.85", "1.25", 70, 25, "UND", "Leche entera pasteurizada"},
                {"Yogur natural x4", 3L, 2L, "1.40", "2.25", 18, 20, "PACK", "Pack de cuatro yogures naturales"},
                {"Detergente liquido 3 L", 5L, 3L, "5.60", "8.90", 25, 5, "UND", "Detergente liquido para ropa blanca y color"},
                {"Atun en lata 165 g", 6L, 2L, "1.15", "1.95", 150, 30, "UND", "Atun en aceite de oliva"},
                {"Aceite de oliva 1 L", 4L, 2L, "4.80", "6.50", 30, 10, "UND", "Aceite de oliva virgen extra"}
        };

        LocalDateTime ahora = LocalDateTime.now().minusDays(7);
        long id = 1;
        long baseEan = 840000000000L;

        for (Object[] d : datos) {
            Producto p = new Producto();
            p.setId(id);
            p.setNombre((String) d[0]);
            p.setCategoriaId((Long) d[1]);
            p.setProveedorId((Long) d[2]);
            p.setPrecioCompra(new BigDecimal((String) d[3]));
            p.setPrecioVenta(new BigDecimal((String) d[4]));
            p.setStock((Integer) d[5]);
            p.setStockMinimo((Integer) d[6]);
            p.setUnidad((String) d[7]);
            p.setDescripcion((String) d[8]);
            p.setCodigoBarras(ServicioCodigoBarras.generarEan13(baseEan + id));
            p.setActivo(true);
            p.setFechaRegistro(ahora.plusMinutes(id));
            Producto guardado = FactoriaRepositorios.productosJson().guardar(p);

            // Movimiento de stock inicial para que el historial sea coherente
            Movimiento m = new Movimiento();
            m.setProductoId(guardado.getId());
            m.setProductoNombre(guardado.getNombre());
            m.setTipo(TipoMovimiento.ENTRADA);
            m.setCantidad(guardado.getStock());
            m.setStockAnterior(0);
            m.setStockResultante(guardado.getStock());
            m.setMotivo("Stock inicial del inventario");
            m.setUsuarioNombre("sistema");
            m.setFecha(guardado.getFechaRegistro());
            FactoriaRepositorios.movimientosJson().guardar(m);

            id++;
        }

        // Algunos movimientos adicionales para que el listado tenga historia
        List<String> motivos = List.of("Compra a proveedor", "Venta mostrador", "Venta mostrador");
        long[] ids = {1L, 3L, 5L};
        int[] cantidades = {20, 10, 15};
        for (int i = 0; i < ids.length; i++) {
            var producto = FactoriaRepositorios.productosJson().buscarPorId(ids[i]).orElse(null);
            if (producto == null) {
                continue;
            }
            int anterior = producto.getStock();
            int cantidad = cantidades[i];
            producto.setStock(anterior - cantidad);
            FactoriaRepositorios.productosJson().guardar(producto);

            Movimiento m = new Movimiento();
            m.setProductoId(producto.getId());
            m.setProductoNombre(producto.getNombre());
            m.setTipo(TipoMovimiento.SALIDA);
            m.setCantidad(cantidad);
            m.setStockAnterior(anterior);
            m.setStockResultante(anterior - cantidad);
            m.setMotivo(motivos.get(i));
            m.setUsuarioNombre("sistema");
            m.setFecha(LocalDateTime.now().minusDays(2).plusHours(i));
            FactoriaRepositorios.movimientosJson().guardar(m);
        }
    }
}
