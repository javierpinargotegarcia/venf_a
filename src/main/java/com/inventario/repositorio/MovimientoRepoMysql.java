package com.inventario.repositorio;

import com.inventario.db.ConexionMySQL;
import com.inventario.modelo.Movimiento;
import com.inventario.modelo.TipoMovimiento;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MovimientoRepoMysql implements MovimientoRepo {

    private static final String COLS =
            "id, producto_id, producto_nombre, tipo, cantidad, stock_anterior, "
                    + "stock_resultante, motivo, usuario_id, usuario_nombre, fecha";

    private final ConexionMySQL db;

    public MovimientoRepoMysql(ConexionMySQL db) {
        this.db = db;
    }

    @Override
    public List<Movimiento> listarTodos() {
        List<Movimiento> lista = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM movimiento ORDER BY fecha DESC, id DESC";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al leer movimientos: " + e.getMessage(), e);
        }
        lista.sort(Comparator.comparing(Movimiento::getFecha, Comparator.nullsLast(Comparator.reverseOrder())));
        return lista;
    }

    @Override
    public List<Movimiento> listarPorProducto(Long productoId) {
        if (productoId == null) return List.of();
        List<Movimiento> lista = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM movimiento WHERE producto_id = ? "
                + "ORDER BY fecha DESC, id DESC";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setLong(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al leer movimientos: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public Optional<Movimiento> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT " + COLS + " FROM movimiento WHERE id = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar movimiento: " + e.getMessage(), e);
        }
    }

    @Override
    public Movimiento guardar(Movimiento m) {
        if (m.getId() == null) {
            m.setId(siguienteId());
        }
        String upsert = "INSERT INTO movimiento (" + COLS + ") VALUES "
                + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "producto_id = VALUES(producto_id), producto_nombre = VALUES(producto_nombre), "
                + "tipo = VALUES(tipo), cantidad = VALUES(cantidad), "
                + "stock_anterior = VALUES(stock_anterior), stock_resultante = VALUES(stock_resultante), "
                + "motivo = VALUES(motivo), usuario_id = VALUES(usuario_id), "
                + "usuario_nombre = VALUES(usuario_nombre), fecha = VALUES(fecha)";
        try (PreparedStatement ps = db.getConexion().prepareStatement(upsert)) {
            ps.setLong(1, m.getId());
            ps.setLong(2, m.getProductoId());
            ps.setString(3, m.getProductoNombre());
            ps.setString(4, m.getTipo().name());
            ps.setInt(5, m.getCantidad());
            ps.setInt(6, m.getStockAnterior());
            ps.setInt(7, m.getStockResultante());
            ps.setString(8, m.getMotivo());
            if (m.getUsuarioId() == null) {
                ps.setObject(9, null);
            } else {
                ps.setLong(9, m.getUsuarioId());
            }
            ps.setString(10, m.getUsuarioNombre());
            ps.setObject(11, m.getFecha());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error al guardar movimiento: " + e.getMessage(), e);
        }
        return m;
    }

    @Override
    public boolean eliminar(Long id) {
        String sql = "DELETE FROM movimiento WHERE id = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al eliminar movimiento: " + e.getMessage(), e);
        }
    }

    @Override
    public long siguienteId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM movimiento";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 1L;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al calcular el id de movimiento: " + e.getMessage(), e);
        }
    }

    private Movimiento mapear(ResultSet rs) throws SQLException {
        Movimiento m = new Movimiento();
        m.setId(rs.getLong("id"));
        m.setProductoId(rs.getLong("producto_id"));
        m.setProductoNombre(rs.getString("producto_nombre"));
        m.setTipo(TipoMovimiento.valueOf(rs.getString("tipo")));
        m.setCantidad(rs.getInt("cantidad"));
        m.setStockAnterior(rs.getInt("stock_anterior"));
        m.setStockResultante(rs.getInt("stock_resultante"));
        m.setMotivo(rs.getString("motivo"));
        long usuarioId = rs.getLong("usuario_id");
        m.setUsuarioId(rs.wasNull() ? null : usuarioId);
        m.setUsuarioNombre(rs.getString("usuario_nombre"));
        m.setFecha(rs.getObject("fecha", LocalDateTime.class));
        return m;
    }
}
