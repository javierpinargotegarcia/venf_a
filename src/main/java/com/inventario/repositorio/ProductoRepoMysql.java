package com.inventario.repositorio;

import com.inventario.db.ConexionMySQL;
import com.inventario.modelo.Producto;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ProductoRepoMysql implements ProductoRepo {

    private static final String COLS =
            "id, codigo_barras, nombre, descripcion, categoria_id, proveedor_id, "
                    + "precio_compra, precio_venta, stock, stock_minimo, unidad, activo, fecha_registro";

    private final ConexionMySQL db;

    public ProductoRepoMysql(ConexionMySQL db) {
        this.db = db;
    }

    @Override
    public List<Producto> listarTodos() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM producto ORDER BY nombre";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al leer productos: " + e.getMessage(), e);
        }
        lista.sort(Comparator.comparing(p -> p.getNombre() == null ? "" : p.getNombre()));
        return lista;
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT " + COLS + " FROM producto WHERE id = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar producto: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Producto> buscarPorCodigoBarras(String codigo) {
        if (codigo == null || codigo.isBlank()) return Optional.empty();
        String sql = "SELECT " + COLS + " FROM producto WHERE codigo_barras = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setString(1, codigo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar producto: " + e.getMessage(), e);
        }
    }

    @Override
    public Producto guardar(Producto p) {
        if (p.getId() == null) {
            p.setId(siguienteId());
        }
        String upsert = "INSERT INTO producto (" + COLS + ") VALUES "
                + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "codigo_barras = VALUES(codigo_barras), nombre = VALUES(nombre), "
                + "descripcion = VALUES(descripcion), categoria_id = VALUES(categoria_id), "
                + "proveedor_id = VALUES(proveedor_id), precio_compra = VALUES(precio_compra), "
                + "precio_venta = VALUES(precio_venta), stock = VALUES(stock), "
                + "stock_minimo = VALUES(stock_minimo), unidad = VALUES(unidad), "
                + "activo = VALUES(activo), fecha_registro = VALUES(fecha_registro)";
        try (PreparedStatement ps = db.getConexion().prepareStatement(upsert)) {
            ps.setLong(1, p.getId());
            ps.setString(2, p.getCodigoBarras());
            ps.setString(3, p.getNombre());
            ps.setString(4, p.getDescripcion());
            setLongO(ps, 5, p.getCategoriaId());
            setLongO(ps, 6, p.getProveedorId());
            ps.setBigDecimal(7, p.getPrecioCompra());
            ps.setBigDecimal(8, p.getPrecioVenta());
            ps.setInt(9, p.getStock());
            ps.setInt(10, p.getStockMinimo());
            ps.setString(11, p.getUnidad());
            ps.setBoolean(12, p.isActivo());
            ps.setObject(13, p.getFechaRegistro());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error al guardar producto: " + e.getMessage(), e);
        }
        return p;
    }

    @Override
    public boolean eliminar(Long id) {
        String sql = "DELETE FROM producto WHERE id = ?";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo eliminar el producto (¿tiene movimientos asociados?): "
                            + e.getMessage(), e);
        }
    }

    @Override
    public long siguienteId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM producto";
        try (PreparedStatement ps = db.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 1L;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al calcular el id de producto: " + e.getMessage(), e);
        }
    }

    private static void setLongO(PreparedStatement ps, int indice, Long valor) throws SQLException {
        if (valor == null) {
            ps.setObject(indice, null);
        } else {
            ps.setLong(indice, valor);
        }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getLong("id"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        long cat = rs.getLong("categoria_id");
        p.setCategoriaId(rs.wasNull() ? null : cat);
        long prov = rs.getLong("proveedor_id");
        p.setProveedorId(rs.wasNull() ? null : prov);
        p.setPrecioCompra(rs.getBigDecimal("precio_compra"));
        p.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        p.setStock(rs.getInt("stock"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setUnidad(rs.getString("unidad"));
        p.setActivo(rs.getBoolean("activo"));
        LocalDateTime fecha = rs.getObject("fecha_registro", LocalDateTime.class);
        p.setFechaRegistro(fecha);
        return p;
    }
}
