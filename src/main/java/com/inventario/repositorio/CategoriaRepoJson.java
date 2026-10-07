package com.inventario.repositorio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.inventario.modelo.Categoria;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Almacena las categorias en {@code data/categorias.json} */
public class CategoriaRepoJson extends BaseJsonRepo<Categoria> implements CategoriaRepo {

    public CategoriaRepoJson(Path carpeta) {
        super(carpeta, "categorias.json", new TypeReference<List<Categoria>>() {
        });
    }

    @Override
    public List<Categoria> listarTodas() {
        return listarTodos();
    }

    @Override
    public Optional<Categoria> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }
        String objetivo = nombre.trim().toLowerCase(Locale.ROOT);
        return datos().stream()
                .filter(c -> c.getNombre() != null
                        && c.getNombre().trim().toLowerCase(Locale.ROOT).equals(objetivo))
                .findFirst();
    }
}
