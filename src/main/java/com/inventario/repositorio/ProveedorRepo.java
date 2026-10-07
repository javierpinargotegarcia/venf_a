package com.inventario.repositorio;

import com.inventario.modelo.Proveedor;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepo {

    List<Proveedor> listarTodos();

    Optional<Proveedor> buscarPorId(Long id);

    Optional<Proveedor> buscarPorNombre(String nombre);

    Proveedor guardar(Proveedor proveedor);

    boolean eliminar(Long id);

    long siguienteId();
}
