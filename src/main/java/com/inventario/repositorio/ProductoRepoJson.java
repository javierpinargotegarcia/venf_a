package com.inventario.repositorio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.inventario.modelo.Producto;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Almacena los productos en {@code data/productos.json} */
public class ProductoRepoJson extends BaseJsonRepo<Producto> implements ProductoRepo {

    public ProductoRepoJson(Path carpeta) {
        super(carpeta, "productos.json", new TypeReference<List<Producto>>() {
        });
    }

    @Override
    public synchronized List<Producto> listarTodos() {
        return datos().stream()
                .sorted(Comparator.comparing(p -> p.getNombre() == null ? "" : p.getNombre()))
                .toList();
    }

    @Override
    public Optional<Producto> buscarPorCodigoBarras(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        String objetivo = codigo.trim();
        return datos().stream()
                .filter(p -> p.getCodigoBarras() != null && p.getCodigoBarras().equalsIgnoreCase(objetivo))
                .findFirst();
    }
}
