package com.inventario.repositorio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventario.modelo.Identificable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Repositorio generico sobre un archivo JSON.
 *
 * <p>El repositorio de ficheros esta organizado por carpetas, por ejemplo:
 * <pre>
 * data/
 * ├── productos.json
 * ├── categorias.json
 * ├── proveedores.json
 * ├── movimientos.json
 * ├── usuarios.json
 * └── codigos_barras/   (imagenes PNG generadas)
 * </pre>
 *
 * <p>Las escrituras son atomicas: primero se escribe un archivo temporal
 * y despues se sustituye al definitivo, de modo que un corte de luz no
 * puede dejar el fichero a medias.
 *
 * @param <T> tipo de entidad almacenada
 */
public abstract class BaseJsonRepo<T extends Identificable> {

    private final ObjectMapper mapper;
    private final Path archivo;
    private final TypeReference<List<T>> tipoLista;
    private List<T> cache;

    protected BaseJsonRepo(Path carpeta, String nombreArchivo, TypeReference<List<T>> tipoLista) {
        this.tipoLista = tipoLista;
        this.archivo = carpeta.resolve(nombreArchivo);
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Tolera campos derivados o de versiones anteriores del fichero
                // (p. ej. "stockBajo" o "valorStock") en vez de fallar al leer.
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** Ruta completa del fichero JSON de este repositorio. */
    public Path getArchivo() {
        return archivo;
    }

    public synchronized List<T> listarTodos() {
        return new ArrayList<>(datos());
    }

    public synchronized Optional<T> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return datos().stream().filter(e -> Objects.equals(e.getId(), id)).findFirst();
    }

    /** Inserta (si id es nulo) o actualiza la entidad. */
    public synchronized T guardar(T entidad) {
        List<T> datos = datos();
        if (entidad.getId() == null) {
            entidad.setId(siguienteId());
        }
        boolean existe = false;
        for (int i = 0; i < datos.size(); i++) {
            if (Objects.equals(datos.get(i).getId(), entidad.getId())) {
                datos.set(i, entidad);
                existe = true;
                break;
            }
        }
        if (!existe) {
            datos.add(entidad);
        }
        persistir();
        return entidad;
    }

    public synchronized boolean eliminar(Long id) {
        List<T> datos = datos();
        boolean borrado = datos.removeIf(e -> Objects.equals(e.getId(), id));
        if (borrado) {
            persistir();
        }
        return borrado;
    }

    public synchronized long siguienteId() {
        return datos().stream()
                .map(Identificable::getId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1;
    }

    public synchronized boolean estaVacio() {
        return datos().isEmpty();
    }

    // ------------------------------------------------------------------ //

    /** Carga el fichero en memoria (la cache se invalida con {@link #recargar()}). */
    protected synchronized List<T> datos() {
        if (cache == null) {
            cache = leer();
        }
        return cache;
    }

    /** Vuelve a leer el fichero del disco (util si otro proceso lo ha modificado). */
    public synchronized void recargar() {
        cache = null;
    }

    private List<T> leer() {
        if (!Files.exists(archivo)) {
            return new ArrayList<>();
        }
        try {
            byte[] contenido = Files.readAllBytes(archivo);
            if (contenido.length == 0) {
                return new ArrayList<>();
            }
            List<T> lista = mapper.readValue(new String(contenido, StandardCharsets.UTF_8), tipoLista);
            return lista == null ? new ArrayList<>() : new ArrayList<>(lista);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer el fichero del repositorio: " + archivo.toAbsolutePath()
                            + "\n" + e.getMessage(), e);
        }
    }

    private void persistir() {
        try {
            Path padre = archivo.getParent();
            if (padre != null) {
                Files.createDirectories(padre);
            }
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            Files.write(temporal, mapper.writeValueAsBytes(cache));
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicoNoSoportado) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo escribir el fichero del repositorio: " + archivo.toAbsolutePath()
                            + "\n" + e.getMessage(), e);
        }
    }
}
