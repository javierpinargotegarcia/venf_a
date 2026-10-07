package com.inventario.repositorio;

import com.inventario.db.ConexionMySQL;
import com.inventario.modelo.RolUsuario;
import com.inventario.modelo.Usuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class UsuarioRepoMysql implements UsuarioRepo {

    private static final String COLS =
            "id, usuario, password_hash, nombre_completo, rol, activo, fecha_registro";

    private final ConexionMySQL db;

    public UsuarioRepoMysql(ConexionMySQL db) {
        this.db = db;
    }

    @Override
    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM usuario ORDER BY usuario";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al leer usuarios: " + e.getMessage(), e);
        }
        lista.sort(Comparator.comparing(u -> u.getUsuario() == null ? "" : u.getUsuario()));
        return lista;
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT " + COLS + " FROM usuario WHERE id = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) return Optional.empty();
        String sql = "SELECT " + COLS + " FROM usuario WHERE LOWER(usuario) = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setString(1, usuario.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public Usuario guardar(Usuario u) {
        if (u.getId() == null) {
            u.setId(siguienteId());
        }
        String upsert = "INSERT INTO usuario (" + COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE usuario = VALUES(usuario), "
                + "password_hash = VALUES(password_hash), nombre_completo = VALUES(nombre_completo), "
                + "rol = VALUES(rol), activo = VALUES(activo), fecha_registro = VALUES(fecha_registro)";
        try (PreparedStatement ps = db.getConexion().prepareStatement(upsert)) {
            ps.setLong(1, u.getId());
            ps.setString(2, u.getUsuario());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getNombreCompleto());
            ps.setString(5, u.getRol().name());
            ps.setBoolean(6, u.isActivo());
            ps.setObject(7, u.getFechaRegistro());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error al guardar usuario: " + e.getMessage(), e);
        }
        return u;
    }

    @Override
    public boolean eliminar(Long id) {
        String sql = "DELETE FROM usuario WHERE id = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al eliminar usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public long siguienteId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM usuario";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 1L;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al calcular el id de usuario: " + e.getMessage(), e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getLong("id"));
        u.setUsuario(rs.getString("usuario"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setRol(RolUsuario.valueOf(rs.getString("rol")));
        u.setActivo(rs.getBoolean("activo"));
        u.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
        return u;
    }
}
