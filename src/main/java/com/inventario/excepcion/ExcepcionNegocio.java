package com.inventario.excepcion;

/**
 * Error de negocio (validacion, stock insuficiente, dato duplicado...).
 * Se lanza con un mensaje pensado para mostrarse directamente al usuario.
 */
public class ExcepcionNegocio extends Exception {

    public ExcepcionNegocio(String mensaje) {
        super(mensaje);
    }

    public ExcepcionNegocio(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
