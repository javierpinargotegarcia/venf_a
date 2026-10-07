package com.inventario.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Carga y guarda el archivo {@code config.properties} de la carpeta de
 * ejecucion. Si no existe, se copia el que viene en los recursos.
 *
 * <p>El campo {@code modo} indica donde se guarda TODO el inventario:
 * <ul>
 *   <li>{@code json}  -> archivos .json organizados en carpetas (repositorio inicial)</li>
 *   <li>{@code mysql} -> base de datos MySQL (tras la migracion)</li>
 * </ul>
 */
public class ConfigApp {

    public static final String MODO_JSON = "json";
    public static final String MODO_MYSQL = "mysql";

    private static final String ARCHIVO = "config.properties";
    private static final String RECURSO_DEFECTO = "/config/config.properties";

    private String modo = MODO_JSON;
    private String carpetaDatos = "data";
    private String mysqlUrl = "";
    private String mysqlUsuario = "root";
    private String mysqlPassword = "";

    private final Path archivoConfig;

    private ConfigApp(Path archivoConfig) {
        this.archivoConfig = archivoConfig;
    }

    /** Carga la configuracion del disco (la crea con valores por defecto si no existe). */
    public static ConfigApp cargar() {
        Path ruta = Path.of(ARCHIVO);
        ConfigApp config = new ConfigApp(ruta);

        if (!Files.exists(ruta)) {
            try (InputStream in = ConfigApp.class.getResourceAsStream(RECURSO_DEFECTO)) {
                if (in != null) {
                    Files.copy(in, ruta);
                } else {
                    config.modo = MODO_JSON;
                    config.guardar();
                    return config;
                }
            } catch (IOException e) {
                System.err.println("No se pudo crear " + ARCHIVO + ": " + e.getMessage());
            }
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(ruta)) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("No se pudo leer " + ARCHIVO + ": " + e.getMessage());
        }

        config.modo = props.getProperty("modo", MODO_JSON).trim().toLowerCase();
        config.carpetaDatos = props.getProperty("carpetaDatos", "data").trim();
        config.mysqlUrl = props.getProperty("mysql.url", "").trim();
        config.mysqlUsuario = props.getProperty("mysql.usuario", "root").trim();
        config.mysqlPassword = props.getProperty("mysql.password", "");
        return config;
    }

    /** Escribe la configuracion actual en disco. */
    public void guardar() {
        Properties props = new Properties();
        props.setProperty("modo", modo);
        props.setProperty("carpetaDatos", carpetaDatos);
        props.setProperty("mysql.url", mysqlUrl);
        props.setProperty("mysql.usuario", mysqlUsuario);
        props.setProperty("mysql.password", mysqlPassword);

        try (OutputStream out = Files.newOutputStream(archivoConfig)) {
            props.store(out, " Configuracion del Sistema de Inventario (se genera automaticamente)");
        } catch (IOException e) {
            System.err.println("No se pudo guardar " + ARCHIVO + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //

    public String getModo() {
        return modo;
    }

    public void setModo(String modo) {
        this.modo = modo;
    }

    public boolean esModoMysql() {
        return MODO_MYSQL.equals(modo);
    }

    public String getCarpetaDatos() {
        return carpetaDatos;
    }

    public void setCarpetaDatos(String carpetaDatos) {
        this.carpetaDatos = carpetaDatos;
    }

    /** Carpeta raiz del repositorio de archivos JSON (ej. {@code data}). */
    public Path getDirectorioDatos() {
        return Path.of(carpetaDatos);
    }

    public String getMysqlUrl() {
        return mysqlUrl;
    }

    public void setMysqlUrl(String mysqlUrl) {
        this.mysqlUrl = mysqlUrl;
    }

    public String getMysqlUsuario() {
        return mysqlUsuario;
    }

    public void setMysqlUsuario(String mysqlUsuario) {
        this.mysqlUsuario = mysqlUsuario;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public void setMysqlPassword(String mysqlPassword) {
        this.mysqlPassword = mysqlPassword;
    }
}
