package com.inventario.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Producto del inventario. El codigo de barras es unico en todo el sistema.
 */
public class Producto implements Identificable {

    private Long id;
    private String codigoBarras;
    private String nombre;
    private String descripcion;
    private Long categoriaId;
    private Long proveedorId;
    private BigDecimal precioCompra = BigDecimal.ZERO;
    private BigDecimal precioVenta = BigDecimal.ZERO;
    private int stock;
    private int stockMinimo;
    private String unidad = "UND";
    private boolean activo = true;
    private LocalDateTime fechaRegistro;

    public Producto() {
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra == null ? BigDecimal.ZERO : precioCompra;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta == null ? BigDecimal.ZERO : precioVenta;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    /** Valor total del stock de este producto: stock x precio de compra. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public BigDecimal getValorStock() {
        return precioCompra.multiply(BigDecimal.valueOf(stock));
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isStockBajo() {
        return stock <= stockMinimo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Producto)) return false;
        Producto that = (Producto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return (nombre == null ? "" : nombre) + " [" + (codigoBarras == null ? "" : codigoBarras) + "]";
    }
}
