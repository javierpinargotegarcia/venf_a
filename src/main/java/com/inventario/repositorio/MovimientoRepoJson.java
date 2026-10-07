package com.inventario.repositorio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.inventario.modelo.Movimiento;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Almacena los movimientos de stock en {@code data/movimientos.json} */
public class MovimientoRepoJson extends BaseJsonRepo<Movimiento> implements MovimientoRepo {

    public MovimientoRepoJson(Path carpeta) {
        super(carpeta, "movimientos.json", new TypeReference<List<Movimiento>>() {
        });
    }

    @Override
    public synchronized List<Movimiento> listarTodos() {
        return datos().stream()
                .sorted(Comparator.comparing(Movimiento::getFecha,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public synchronized List<Movimiento> listarPorProducto(Long productoId) {
        if (productoId == null) {
            return List.of();
        }
        return listarTodos().stream()
                .filter(m -> productoId.equals(m.getProductoId()))
                .toList();
    }
}
