package com.inventario.repositorio;

import com.inventario.db.ConexionMySQL;
import com.inventario.modelo.Categoria;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CategoriaRepoMysql implements CategoriaRepo {

    private final ConexionMySQL db;

    public CategoriaRepoMysql(ConexionMySQL db) {
        this.db = db;
    }

    @Override
    public List<Categoria> listarTodas() {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, descripcion FROM categoria ORDER BY nombre";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al leer categorias: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public Optional<Categoria> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, nombre, descripcion FROM categoria WHERE id = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar categoria: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Categoria> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return Optional.empty();
        String objetivo = nombre.trim().toLowerCase(Locale.ROOT);
        String sql = "SELECT id, nombre, descripcion FROM categoria";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Categoria c = mapear(rs);
                if (c.getNombre() != null
                        && c.getNombre().trim().toLowerCase(Locale.ROOT).equals(objetivo)) {
                    return Optional.of(c);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar categoria: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Categoria guardar(Categoria categoria) {
        if (categoria.getId() == null) {
            categoria.setId(siguienteId());
        }
        String upsert = "INSERT INTO categoria (id, nombre, descripcion) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion)";
        try (PreparedStatement ps = db.getConexion().prepareStatement(upsert)) {
            ps.setLong(1, categoria.getId());
            ps.setString(2, categoria.getNombre());
            ps.setString(3, categoria.getDescripcion());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error al guardar categoria: " + e.getMessage(), e);
        }
        return categoria;
    }

    @Override
    public boolean eliminar(Long id) {
        String sql = "DELETE FROM categoria WHERE id = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo eliminar la categoria (¿tiene productos asociados?): "
                            + e.getMessage(), e);
        }
    }

    @Override
    public long siguienteId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM categoria";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 1L;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al calcular el id de categoria: " + e.getMessage(), e);
        }
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getLong("id"));
        c.setNombre(rs.getString("nombre"));
        c.setDescripcion(rs.getString("descripcion"));
        return c;
    }
}
