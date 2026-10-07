package com.inventario.servicio;

import com.inventario.AppContexto;
import com.inventario.config.ConfigApp;
import com.inventario.db.ConexionMySQL;
import com.inventario.excepcion.ExcepcionNegocio;
import com.inventario.repositorio.FactoriaRepositorios;

import java.util.ArrayList;
import java.util.List;

/**
 * Migra el repositorio de ficheros JSON a la base de datos MySQL.
 *
 * <p>Orden de insercion (para respetar las claves ajenas):
 * categorias -> proveedores -> usuarios -> productos -> movimientos.
 *
 * <p>Los identificadores se conservan tal cual, de modo que las relaciones
 * entre tablas quedan identicas a las del repositorio JSON.
 */
public class ServicioMigracion {

    /** Resultado del proceso, para mostrarlo en pantalla y en el log. */
    public record ResumenMigracion(String servidor,
                                   long categorias,
                                   long proveedores,
                                   long usuarios,
                                   long productos,
                                   long movimientos,
                                   List<String> detalles) {

        public long total() {
            return categorias + proveedores + usuarios + productos + movimientos;
        }
    }

    /** Comprueba que MySQL responde con la configuracion actual. */
    public String probarConexion() throws Exception {
        ConfigApp config = AppContexto.config();
        if (config.getMysqlUrl() == null || config.getMysqlUrl().isBlank()) {
            throw new ExcepcionNegocio(
                    "Falta la url de conexion MySQL en config.properties (mysql.url).");
        }
        return ConexionMySQL.obtener(config).probar();
    }

    /** Comprueba que hay algo que migrar en el repositorio JSON. */
    public boolean hayDatosJson() {
        return !FactoriaRepositorios.productosJson().listarTodos().isEmpty()
                || !FactoriaRepositorios.usuariosJson().listarTodos().isEmpty()
                || !FactoriaRepositorios.categoriasJson().listarTodas().isEmpty();
    }

    /**
     * Copia todo el repositorio JSON a MySQL y deja la aplicacion en modo MySQL.
     * Si algo falla, la aplicacion sigue en modo JSON: no se pierde nada.
     */
    public ResumenMigracion migrarJsonAMysql() throws Exception {
        ConfigApp config = AppContexto.config();

        List<String> detalles = new ArrayList<>();

        // 1) Probar conexion
        String version;
        try {
            version = probarConexion();
        } catch (Exception e) {
            throw new ExcepcionNegocio(
                    "No se pudo conectar con MySQL.\n\n"
                            + "Revisa config.properties (mysql.url, mysql.usuario, mysql.password).\n"
                            + "Detalle: " + e.getMessage(), e);
        }
        detalles.add("Conectado a MySQL " + version);

        // 2) Crear tablas si no existen
        try {
            ConexionMySQL.obtener(config).inicializarEsquema();
            detalles.add("Esquema de base de datos comprobado (CREATE TABLE IF NOT EXISTS)");
        } catch (Exception e) {
            throw new ExcepcionNegocio(
                    "No se pudo preparar el esquema de la base de datos:\n" + e.getMessage(), e);
        }

        // 3) Leer todo del repositorio JSON (forzado, independiente del modo activo)
        var categorias = FactoriaRepositorios.categoriasJson().listarTodas();
        var proveedores = FactoriaRepositorios.proveedoresJson().listarTodos();
        var usuarios = FactoriaRepositorios.usuariosJson().listarTodos();
        var productos = FactoriaRepositorios.productosJson().listarTodos();
        var movimientos = FactoriaRepositorios.movimientosJson().listarTodos();

        if (categorias.isEmpty() && productos.isEmpty() && usuarios.isEmpty()) {
            throw new ExcepcionNegocio(
                    "El repositorio JSON no contiene datos que migrar (" + config.getCarpetaDatos() + ").");
        }

        // 4) Insertar en MySQL preservando los IDs originales
        try {
            for (var c : categorias) {
                FactoriaRepositorios.categoriasMysql().guardar(c);
            }
            detalles.add("Categorias migradas: " + categorias.size());

            for (var p : proveedores) {
                FactoriaRepositorios.proveedoresMysql().guardar(p);
            }
            detalles.add("Proveedores migrados: " + proveedores.size());

            for (var u : usuarios) {
                FactoriaRepositorios.usuariosMysql().guardar(u);
            }
            detalles.add("Usuarios migrados: " + usuarios.size());

            for (var p : productos) {
                FactoriaRepositorios.productosMysql().guardar(p);
            }
            detalles.add("Productos migrados: " + productos.size());

            for (var m : movimientos) {
                FactoriaRepositorios.movimientosMysql().guardar(m);
            }
            detalles.add("Movimientos migrados: " + movimientos.size());
        } catch (Exception e) {
            throw new ExcepcionNegocio(
                    "La migracion se ha interrumpido y NO se ha cambiado el modo de la aplicacion.\n"
                            + "Los datos originales del repositorio JSON siguen intactos.\n\n"
                            + "Detalle: " + e.getMessage(), e);
        }

        // 5) Verificacion: lo que hay en MySQL debe coincidir con el JSON
        long enMysql = FactoriaRepositorios.productosMysql().listarTodos().size();
        if (enMysql != productos.size()) {
            throw new ExcepcionNegocio(
                    "Verificacion fallida: el JSON tiene " + productos.size()
                            + " productos y MySQL " + enMysql + ". La aplicacion sigue en modo JSON.");
        }
        detalles.add("Verificacion correcta: " + enMysql + " productos en MySQL");

        // 6) Cambiar a modo MySQL (se guarda en config.properties)
        FactoriaRepositorios.usarModo(ConfigApp.MODO_MYSQL);
        detalles.add("Modo de almacenamiento cambiado a: mysql");

        return new ResumenMigracion(config.getMysqlUrl(),
                categorias.size(), proveedores.size(), usuarios.size(),
                productos.size(), movimientos.size(), detalles);
    }

    /**
     * Vuelve al modo JSON (los ficheros locales siguen conservados).
     * Util para consultas o para repetir la migracion.
     */
    public void volverAModoJson() {
        FactoriaRepositorios.usarModo(ConfigApp.MODO_JSON);
    }
}
