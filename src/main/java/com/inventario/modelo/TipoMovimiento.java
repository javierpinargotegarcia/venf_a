package com.inventario.modelo;

/**
 * Tipo de movimiento de stock.
 * ENTRADA  = ingreso de mercaderia (compra, devolucion, ajuste positivo)
 * SALIDA   = egreso de mercaderia (venta, roto, merma, ajuste negativo)
 * AJUSTE  = correccion manual del inventario
 */
public enum TipoMovimiento {
    ENTRADA("Entrada"),
    SALIDA("Salida"),
    AJUSTE("Ajuste");

    private final String etiqueta;

    TipoMovimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
