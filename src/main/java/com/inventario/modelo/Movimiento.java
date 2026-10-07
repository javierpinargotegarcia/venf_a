package com.inventario.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Movimiento de stock (entrada, salida o ajuste).
 * Se guarda desnormalizado (nombre de producto y usuario) para que los
 * reportes sean fieles aunque luego se edite o borre el producto.
 */
public class Movimiento implements Identificable {

    private Long id;
    private Long productoId;
    private String productoNombre;
    private TipoMovimiento tipo;
    private int cantidad;
    private int stockAnterior;
    private int stockResultante;
    private String motivo;
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime fecha;

    public Movimiento() {
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public void setProductoNombre(String productoNombre) {
        this.productoNombre = productoNombre;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public int getStockAnterior() {
        return stockAnterior;
    }

    public void setStockAnterior(int stockAnterior) {
        this.stockAnterior = stockAnterior;
    }

    public int getStockResultante() {
        return stockResultante;
    }

    public void setStockResultante(int stockResultante) {
        this.stockResultante = stockResultante;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    /** Devuelve la variacion real de stock (+entrada / -salida). */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getVariacion() {
        return stockResultante - stockAnterior;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movimiento)) return false;
        Movimiento that = (Movimiento) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
