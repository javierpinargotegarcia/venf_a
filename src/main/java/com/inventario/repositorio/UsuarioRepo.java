package com.inventario.repositorio;

import com.inventario.modelo.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepo {

    List<Usuario> listarTodos();

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorUsuario(String usuario);

    Usuario guardar(Usuario usuario);

    boolean eliminar(Long id);

    long siguienteId();
}
