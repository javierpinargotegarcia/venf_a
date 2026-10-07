package com.inventario.repositorio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.inventario.modelo.Usuario;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Almacena los usuarios en {@code data/usuarios.json} */
public class UsuarioRepoJson extends BaseJsonRepo<Usuario> implements UsuarioRepo {

    public UsuarioRepoJson(Path carpeta) {
        super(carpeta, "usuarios.json", new TypeReference<List<Usuario>>() {
        });
    }

    @Override
    public synchronized List<Usuario> listarTodos() {
        return datos().stream()
                .sorted(Comparator.comparing(u -> u.getUsuario() == null ? "" : u.getUsuario()))
                .toList();
    }

    @Override
    public Optional<Usuario> buscarPorUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            return Optional.empty();
        }
        String objetivo = usuario.trim().toLowerCase();
        return datos().stream()
                .filter(u -> u.getUsuario() != null && u.getUsuario().trim().toLowerCase().equals(objetivo))
                .findFirst();
    }
}
