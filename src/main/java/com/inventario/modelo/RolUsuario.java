package com.inventario.modelo;

/** Roles de usuario del sistema. */
public enum RolUsuario {
    ADMIN("Administrador"),
    OPERARIO("Operario");

    private final String etiqueta;

    RolUsuario(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Solo los administradores pueden gestionar usuarios y migrar a MySQL. */
    public boolean esAdmin() {
        return this == ADMIN;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
