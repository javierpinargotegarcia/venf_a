package com.inventario.seguridad;

import com.inventario.modelo.Usuario;

/**
 * Usuario que tiene la sesion abierta en la aplicacion.
 * Se usa para registrar quien realiza cada movimiento de stock.
 */
public final class Sesion {

    private static Usuario usuario;

    private Sesion() {
    }

    public static void abrir(Usuario u) {
        usuario = u;
    }

    public static void cerrar() {
        usuario = null;
    }

    public static Usuario getUsuario() {
        return usuario;
    }

    public static boolean haySesion() {
        return usuario != null;
    }

    public static String getNombreVisible() {
        if (usuario == null) {
            return "";
        }
        return usuario.getNombreCompleto() == null || usuario.getNombreCompleto().isBlank()
                ? usuario.getUsuario()
                : usuario.getNombreCompleto();
    }

    public static boolean esAdmin() {
        return usuario != null && usuario.getRol() != null && usuario.getRol().esAdmin();
    }
}
