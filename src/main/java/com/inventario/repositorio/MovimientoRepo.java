package com.inventario.repositorio;

import com.inventario.modelo.Movimiento;

import java.util.List;
import java.util.Optional;

public interface MovimientoRepo {

    /** Todos los movimientos, del mas reciente al mas antiguo. */
    List<Movimiento> listarTodos();

    List<Movimiento> listarPorProducto(Long productoId);

    Optional<Movimiento> buscarPorId(Long id);

    Movimiento guardar(Movimiento movimiento);

    boolean eliminar(Long id);

    long siguienteId();
}
