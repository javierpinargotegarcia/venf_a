package com.inventario.repositorio;

import com.inventario.modelo.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoRepo {

    /** Todos los productos, incluidos los dados de baja (inactivos). */
    List<Producto> listarTodos();

    Optional<Producto> buscarPorId(Long id);

    Optional<Producto> buscarPorCodigoBarras(String codigo);

    Producto guardar(Producto producto);

    boolean eliminar(Long id);

    long siguienteId();
}
