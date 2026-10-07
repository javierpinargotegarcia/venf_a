package com.inventario.repositorio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.inventario.modelo.Proveedor;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Almacena los proveedores en {@code data/proveedores.json} */
public class ProveedorRepoJson extends BaseJsonRepo<Proveedor> implements ProveedorRepo {

    public ProveedorRepoJson(Path carpeta) {
        super(carpeta, "proveedores.json", new TypeReference<List<Proveedor>>() {
        });
    }

    @Override
    public Optional<Proveedor> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }
        String objetivo = nombre.trim().toLowerCase(Locale.ROOT);
        return datos().stream()
                .filter(p -> p.getNombre() != null
                        && p.getNombre().trim().toLowerCase(Locale.ROOT).equals(objetivo))
                .findFirst();
    }
}
