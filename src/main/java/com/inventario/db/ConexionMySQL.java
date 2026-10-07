package com.inventario.db;

import com.inventario.config.ConfigApp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conexion a MySQL y creacion automatica del esquema.
 * Se usa unicamente cuando la aplicacion esta en modo {@code mysql}
 * o cuando se ejecuta la migracion desde los archivos JSON.
 */
public final class ConexionMySQL {

    private static final String SCRIPT_ESQUEMA = "/db/schema.sql";

    private static ConexionMySQL instancia;

    private final ConfigApp config;
    private Connection conexion;

    private ConexionMySQL(ConfigApp config) {
        this.config = config;
    }

    public static synchronized ConexionMySQL obtener(ConfigApp config) {
        if (instancia == null) {
            instancia = new ConexionMySQL(config);
        }
        return instancia;
    }

    /** Devuelve una conexion abierta (se reutiliza una unica conexion). */
    public synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                // El driver tambien se registra automaticamente via ServiceLoader.
            }
            conexion = DriverManager.getConnection(
                    config.getMysqlUrl(), config.getMysqlUsuario(), config.getMysqlPassword());
        }
        return conexion;
    }

    /** Comprueba que la base de datos responde. Devuelve la version o lanza excepcion. */
    public synchronized String probar() throws SQLException {
        Connection conexion = getConexion();
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SELECT VERSION()")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException sinVersion) {
            // Motor sin la funcion VERSION(): comprobacion generica de acceso.
            try (Statement st = conexion.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    return "OK";
                }
            }
        }
        return "OK";
    }

    /**
     * Ejecuta {@code db/schema.sql} (CREATE TABLE IF NOT EXISTS),
     * por lo que es seguro llamarlo en cada arranque en modo MySQL.
     * Antes se asegura de que la base de datos del URL existe.
     */
    public synchronized void inicializarEsquema() throws SQLException, IOException {
        crearBaseDeDatosSiNoExiste();

        String script;
        try (InputStream in = ConexionMySQL.class.getResourceAsStream(SCRIPT_ESQUEMA)) {
            if (in == null) {
                throw new IOException("No se encuentra el recurso " + SCRIPT_ESQUEMA);
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        ejecutarScript(script);
    }

    /**
     * Crea la base de datos del URL si no existe
     * ({@code CREATE DATABASE IF NOT EXISTS ...}), para que no sea necesario
     * prepararla a mano antes de migrar.
     */
    private void crearBaseDeDatosSiNoExiste() throws SQLException {
        String url = config.getMysqlUrl();
        if (url == null || url.isBlank()) {
            return;
        }
        String sinConsulta = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        int ultimaBarra = sinConsulta.lastIndexOf('/');
        if (ultimaBarra < 0 || ultimaBarra == sinConsulta.length() - 1) {
            return; // sin base de datos concreta en el URL
        }
        String base = sinConsulta.substring(ultimaBarra + 1);
        if (!base.matches("[A-Za-z0-9_]+")) {
            return; // nombre no valido para componer una sentencia
        }
        String urlServidor = sinConsulta.substring(0, ultimaBarra + 1);

        // Conexion temporal "sin base de datos" para poder crearla.
        try (Connection servidor = DriverManager.getConnection(
                urlServidor, config.getMysqlUsuario(), config.getMysqlPassword());
             Statement st = servidor.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS `" + base + "` "
                    + "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            // Fuerza a reconectar contra la base de datos recien creada.
            if (conexion != null && !conexion.isClosed()) {
                try {
                    conexion.close();
                } catch (SQLException ignorado) {
                    // nada que hacer
                }
            }
            conexion = null;
        } catch (SQLException e) {
            // Sin permisos para crear la base de datos: se continua y,
            // si realmente falta, la creacion de tablas dara un error claro.
            System.err.println("[inventario] No se pudo crear la base de datos '" + base
                    + "' (¿faltan permisos?): " + e.getMessage());
        }
    }

    /** Ejecuta un script SQL separado por punto y coma (ignora lineas de comentario). */
    public void ejecutarScript(String script) throws SQLException {
        StringBuilder sentencia = new StringBuilder();
        try (Statement st = getConexion().createStatement()) {
            for (String linea : script.split("\n")) {
                String limpia = linea.trim();
                if (limpia.isEmpty() || limpia.startsWith("--")) {
                    continue;
                }
                sentencia.append(limpia).append('\n');
                if (limpia.endsWith(";")) {
                    st.execute(sentencia.toString().trim());
                    sentencia.setLength(0);
                }
            }
            if (!sentencia.toString().isBlank()) {
                st.execute(sentencia.toString().trim());
            }
        }
    }

    /** Cierra la conexion (al salir de la aplicacion). */
    public static synchronized void cerrar() {
        if (instancia != null && instancia.conexion != null) {
            try {
                instancia.conexion.close();
            } catch (SQLException ignored) {
                // nada que hacer
            }
            instancia.conexion = null;
        }
    }
}
