package com.inventario.servicio;

import com.inventario.excepcion.ExcepcionNegocio;
import com.inventario.modelo.Usuario;
import com.inventario.repositorio.FactoriaRepositorios;
import com.inventario.seguridad.Sesion;
import com.inventario.util.UtilHash;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Login y gestion de usuarios del sistema. */
public class ServicioAutenticacion {

    public static final int LONGITUD_MINIMA_CLAVE = 6;

    /** Valida credenciales y abre sesion. Lanza {@link ExcepcionNegocio} si no son validas. */
    public Usuario autenticar(String usuario, String clave) throws ExcepcionNegocio {
        if (usuario == null || usuario.isBlank()) {
            throw new ExcepcionNegocio("Introduce el nombre de usuario.");
        }
        if (clave == null || clave.isBlank()) {
            throw new ExcepcionNegocio("Introduce la contrasena.");
        }

        Optional<Usuario> buscado = FactoriaRepositorios.usuarios().buscarPorUsuario(usuario.trim());
        if (buscado.isEmpty() || !UtilHash.verificar(clave, buscado.get().getPasswordHash())) {
            throw new ExcepcionNegocio("Usuario o contrasena incorrectos.");
        }
        Usuario u = buscado.get();
        if (!u.isActivo()) {
            throw new ExcepcionNegocio("Este usuario esta desactivado. Contacta con el administrador.");
        }
        Sesion.abrir(u);
        return u;
    }

    public void cerrarSesion() {
        Sesion.cerrar();
    }

    public List<Usuario> listar() {
        return FactoriaRepositorios.usuarios().listarTodos();
    }

    /**
     * Crea o actualiza un usuario.
     *
     * @param claveNueva contrasena nueva; si esta vacia en una edicion se conserva la actual
     */
    public Usuario guardar(Usuario usuario, String claveNueva) throws ExcepcionNegocio {
        if (usuario.getUsuario() == null || usuario.getUsuario().isBlank()) {
            throw new ExcepcionNegocio("El nombre de usuario es obligatorio.");
        }
        if (usuario.getNombreCompleto() == null || usuario.getNombreCompleto().isBlank()) {
            throw new ExcepcionNegocio("El nombre completo es obligatorio.");
        }
        if (usuario.getRol() == null) {
            throw new ExcepcionNegocio("Selecciona un rol.");
        }

        String nombre = usuario.getUsuario().trim();
        usuario.setUsuario(nombre);

        boolean esNuevo = usuario.getId() == null;
        if (esNuevo) {
            if (claveNueva == null || claveNueva.length() < LONGITUD_MINIMA_CLAVE) {
                throw new ExcepcionNegocio(
                        "La contrasena debe tener al menos " + LONGITUD_MINIMA_CLAVE + " caracteres.");
            }
            usuario.setPasswordHash(UtilHash.crear(claveNueva));
            usuario.setFechaRegistro(LocalDateTime.now());
        } else {
            if (claveNueva != null && !claveNueva.isBlank()) {
                if (claveNueva.length() < LONGITUD_MINIMA_CLAVE) {
                    throw new ExcepcionNegocio(
                            "La contrasena debe tener al menos " + LONGITUD_MINIMA_CLAVE + " caracteres.");
                }
                usuario.setPasswordHash(UtilHash.crear(claveNueva));
            }
        }

        Optional<Usuario> duplicado = FactoriaRepositorios.usuarios().buscarPorUsuario(nombre);
        if (duplicado.isPresent() && !duplicado.get().getId().equals(usuario.getId())) {
            throw new ExcepcionNegocio("Ya existe un usuario llamado \"" + nombre + "\".");
        }

        if (usuario.getFechaRegistro() == null) {
            usuario.setFechaRegistro(LocalDateTime.now());
        }
        return FactoriaRepositorios.usuarios().guardar(usuario);
    }

    /** Desactiva (baja logica) un usuario sin perder su historial. */
    public void desactivar(Long id) throws ExcepcionNegocio {
        Usuario objetivo = FactoriaRepositorios.usuarios().buscarPorId(id)
                .orElseThrow(() -> new ExcepcionNegocio("El usuario ya no existe."));

        if (Sesion.haySesion() && id.equals(Sesion.getUsuario().getId())) {
            throw new ExcepcionNegocio("No puedes desactivar tu propia sesion.");
        }
        if (objetivo.getRol() != null && objetivo.getRol().esAdmin() && objetivo.isActivo()) {
            long admins = listar().stream()
                    .filter(u -> u.isActivo() && u.getRol() != null && u.getRol().esAdmin())
                    .count();
            if (admins <= 1) {
                throw new ExcepcionNegocio("Debe existir al menos un administrador activo.");
            }
        }
        objetivo.setActivo(false);
        FactoriaRepositorios.usuarios().guardar(objetivo);
    }

    public void activar(Long id) throws ExcepcionNegocio {
        Usuario objetivo = FactoriaRepositorios.usuarios().buscarPorId(id)
                .orElseThrow(() -> new ExcepcionNegocio("El usuario ya no existe."));
        objetivo.setActivo(true);
        FactoriaRepositorios.usuarios().guardar(objetivo);
    }
}
