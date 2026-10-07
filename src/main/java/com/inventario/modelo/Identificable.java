package com.inventario.modelo;

/**
 * Contrato comun de todas las entidades del inventario: poseen un ID numerico.
 * Permite tratarlas de forma generica desde el repositorio JSON y desde MySQL.
 */
public interface Identificable {

    Long getId();

    void setId(Long id);
}
