package com.inventario.repositorio;

import com.inventario.config.ConfigApp;
import com.inventario.db.ConexionMySQL;

/**
 * Fabrica de repositorios.
 *
 * <p>Mantiene dos juegos completos de repositorios:
 * <ul>
 *   <li><b>JSON</b>: archivos .json en carpetas (repositorio original)</li>
 *   <li><b>MySQL</b>: base de datos (destino de la migracion)</li>
 * </ul>
 * y entrega el juego activo segun el modo configurado. De esta forma se
 * puede migrar de JSON a MySQL sin reiniciar la aplicacion.
 */
public final class FactoriaRepositorios {

    private static ConfigApp config;
    private static boolean modoMysql;

    private static Juego juegoJson;
    private static Juego juegoMysql;

    private FactoriaRepositorios() {
    }

    public static void inicializar(ConfigApp cfg) {
        config = cfg;
        modoMysql = cfg.esModoMysql();
        juegoJson = null;
        juegoMysql = null;
    }

    /** Cambia el almacenamiento activo en tiempo de ejecucion. */
    public static synchronized void usarModo(String modo) {
        modoMysql = ConfigApp.MODO_MYSQL.equals(modo);
        if (config != null) {
            config.setModo(modo);
            config.guardar();
        }
    }

    public static synchronized boolean esModoMysql() {
        return modoMysql;
    }

    // ------------------------- juego ACTIVO ------------------------- //

    public static CategoriaRepo categorias() {
        return modoMysql ? mysql().categorias : json().categorias;
    }

    public static ProveedorRepo proveedores() {
        return modoMysql ? mysql().proveedores : json().proveedores;
    }

    public static ProductoRepo productos() {
        return modoMysql ? mysql().productos : json().productos;
    }

    public static MovimientoRepo movimientos() {
        return modoMysql ? mysql().movimientos : json().movimientos;
    }

    public static UsuarioRepo usuarios() {
        return modoMysql ? mysql().usuarios : json().usuarios;
    }

    // --------------------- juego JSON explicito -------------------- //

    public static CategoriaRepo categoriasJson() {
        return json().categorias;
    }

    public static ProveedorRepo proveedoresJson() {
        return json().proveedores;
    }

    public static ProductoRepo productosJson() {
        return json().productos;
    }

    public static MovimientoRepo movimientosJson() {
        return json().movimientos;
    }

    public static UsuarioRepo usuariosJson() {
        return json().usuarios;
    }

    // --------------------- juego MySQL explicito ------------------- //

    public static CategoriaRepo categoriasMysql() {
        return mysql().categorias;
    }

    public static ProveedorRepo proveedoresMysql() {
        return mysql().proveedores;
    }

    public static ProductoRepo productosMysql() {
        return mysql().productos;
    }

    public static MovimientoRepo movimientosMysql() {
        return mysql().movimientos;
    }

    public static UsuarioRepo usuariosMysql() {
        return mysql().usuarios;
    }

    // ---------------------------------------------------------------- //

    private static synchronized Juego json() {
        if (juegoJson == null) {
            java.nio.file.Path carpeta = config.getDirectorioDatos();
            juegoJson = new Juego(
                    new CategoriaRepoJson(carpeta),
                    new ProveedorRepoJson(carpeta),
                    new ProductoRepoJson(carpeta),
                    new MovimientoRepoJson(carpeta),
                    new UsuarioRepoJson(carpeta));
        }
        return juegoJson;
    }

    private static synchronized Juego mysql() {
        if (juegoMysql == null) {
            ConexionMySQL conexion = ConexionMySQL.obtener(config);
            juegoMysql = new Juego(
                    new CategoriaRepoMysql(conexion),
                    new ProveedorRepoMysql(conexion),
                    new ProductoRepoMysql(conexion),
                    new MovimientoRepoMysql(conexion),
                    new UsuarioRepoMysql(conexion));
        }
        return juegoMysql;
    }

    /** Conjunto completo de repositorios de un mismo almacenamiento. */
    private static final class Juego {
        final CategoriaRepo categorias;
        final ProveedorRepo proveedores;
        final ProductoRepo productos;
        final MovimientoRepo movimientos;
        final UsuarioRepo usuarios;

        Juego(CategoriaRepo categorias, ProveedorRepo proveedores, ProductoRepo productos,
              MovimientoRepo movimientos, UsuarioRepo usuarios) {
            this.categorias = categorias;
            this.proveedores = proveedores;
            this.productos = productos;
            this.movimientos = movimientos;
            this.usuarios = usuarios;
        }
    }
}
