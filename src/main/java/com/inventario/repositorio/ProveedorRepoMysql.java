package com.inventario.repositorio;

import com.inventario.db.ConexionMySQL;
import com.inventario.modelo.Proveedor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ProveedorRepoMysql implements ProveedorRepo {

    private final ConexionMySQL db;

    public ProveedorRepoMysql(ConexionMySQL db) {
        this.db = db;
    }

    @Override
    public List<Proveedor> listarTodos() {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, contacto, telefono, email, direccion "
                + "FROM proveedor ORDER BY nombre";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al leer proveedores: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public Optional<Proveedor> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, nombre, contacto, telefono, email, direccion "
                + "FROM proveedor WHERE id = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar proveedor: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Proveedor> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return Optional.empty();
        String objetivo = nombre.trim().toLowerCase(Locale.ROOT);
        for (Proveedor p : listarTodos()) {
            if (p.getNombre() != null
                    && p.getNombre().trim().toLowerCase(Locale.ROOT).equals(objetivo)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    @Override
    public Proveedor guardar(Proveedor proveedor) {
        if (proveedor.getId() == null) {
            proveedor.setId(siguienteId());
        }
        String upsert = "INSERT INTO proveedor (id, nombre, contacto, telefono, email, direccion) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "nombre = VALUES(nombre), contacto = VALUES(contacto), telefono = VALUES(telefono), "
                + "email = VALUES(email), direccion = VALUES(direccion)";
        try (PreparedStatement ps = db.getConexion().prepareStatement(upsert)) {
            ps.setLong(1, proveedor.getId());
            ps.setString(2, proveedor.getNombre());
            ps.setString(3, proveedor.getContacto());
            ps.setString(4, proveedor.getTelefono());
            ps.setString(5, proveedor.getEmail());
            ps.setString(6, proveedor.getDireccion());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error al guardar proveedor: " + e.getMessage(), e);
        }
        return proveedor;
    }

    @Override
    public boolean eliminar(Long id) {
        String sql = "DELETE FROM proveedor WHERE id = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo eliminar el proveedor (¿tiene productos asociados?): "
                            + e.getMessage(), e);
        }
    }

    @Override
    public long siguienteId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM proveedor";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 1L;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al calcular el id de proveedor: " + e.getMessage(), e);
        }
    }

    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setId(rs.getLong("id"));
        p.setNombre(rs.getString("nombre"));
        p.setContacto(rs.getString("contacto"));
        p.setTelefono(rs.getString("telefono"));
        p.setEmail(rs.getString("email"));
        p.setDireccion(rs.getString("direccion"));
        return p;
    }
}
