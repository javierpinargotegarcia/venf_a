package com.inventario;

import com.inventario.config.ConfigApp;
import com.inventario.repositorio.FactoriaRepositorios;
import com.inventario.servicio.ServicioAutenticacion;
import com.inventario.servicio.ServicioCodigoBarras;
import com.inventario.servicio.ServicioInventario;
import com.inventario.servicio.ServicioMigracion;
import com.inventario.servicio.ServicioReportes;

/**
 * Punto de acceso unico a la configuracion y a los servicios.
 * Los servicios consultan siempre a {@link FactoriaRepositorios}, de modo
 * que al migrar a MySQL cambian de fuente de datos sin reiniciar.
 */
public final class AppContexto {

    private static ConfigApp config;
    private static ServicioInventario inventario;
    private static ServicioAutenticacion autenticacion;
    private static ServicioReportes reportes;
    private static ServicioCodigoBarras codigos;
    private static ServicioMigracion migracion;

    private AppContexto() {
    }

    public static void inicializar(ConfigApp cfg) {
        config = cfg;
        FactoriaRepositorios.inicializar(cfg);
        inventario = null;
        autenticacion = null;
        reportes = null;
        codigos = null;
        migracion = null;
    }

    public static ConfigApp config() {
        return config;
    }

    public static synchronized ServicioInventario inventario() {
        if (inventario == null) {
            inventario = new ServicioInventario();
        }
        return inventario;
    }

    public static synchronized ServicioAutenticacion autenticacion() {
        if (autenticacion == null) {
            autenticacion = new ServicioAutenticacion();
        }
        return autenticacion;
    }

    public static synchronized ServicioReportes reportes() {
        if (reportes == null) {
            reportes = new ServicioReportes();
        }
        return reportes;
    }

    public static synchronized ServicioCodigoBarras codigos() {
        if (codigos == null) {
            codigos = new ServicioCodigoBarras();
        }
        return codigos;
    }

    public static synchronized ServicioMigracion migracion() {
        if (migracion == null) {
            migracion = new ServicioMigracion();
        }
        return migracion;
    }
}
