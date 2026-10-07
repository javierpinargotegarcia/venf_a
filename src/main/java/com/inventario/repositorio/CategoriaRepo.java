package com.inventario.repositorio;

import com.inventario.modelo.Categoria;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepo {

    List<Categoria> listarTodas();

    Optional<Categoria> buscarPorId(Long id);

    Optional<Categoria> buscarPorNombre(String nombre);

    Categoria guardar(Categoria categoria);

    boolean eliminar(Long id);

    long siguienteId();
}
