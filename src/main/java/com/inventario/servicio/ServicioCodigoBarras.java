package com.inventario.servicio;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.inventario.AppContexto;
import com.inventario.repositorio.FactoriaRepositorios;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Generacion y lectura de codigos de barras.
 *
 * <ul>
 *   <li>Codigos de 13 digitos validos -> <b>EAN-13</b> (lectores de supermercado)</li>
 *   <li>Cualquier otro texto -> <b>Code-128</b> (etiquetas internas)</li>
 * </ul>
 */
public class ServicioCodigoBarras {

    public static final String CARPETA_IMAGENES = "codigos_barras";

    private static final int ANCHO_DEFECTO = 400;
    private static final int ALTO_DEFECTO = 120;

    // ================================================================== //
    //                       GENERACION DE IMAGEN                         //
    // ================================================================== //

    /** Imagen del codigo de barras con el tamano por defecto. */
    public BufferedImage generarImagen(String codigo) throws Exception {
        return generarImagen(codigo, ANCHO_DEFECTO, ALTO_DEFECTO);
    }

    /** Imagen del codigo de barras. El formato se elige segun el contenido. */
    public BufferedImage generarImagen(String codigo, int ancho, int alto) throws Exception {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El codigo de barras esta vacio.");
        }
        String texto = codigo.trim();
        BarcodeFormat formato = esEan13Valido(texto) ? BarcodeFormat.EAN_13 : BarcodeFormat.CODE_128;

        // Ancho minimo para que EAN-13 sea escaneable
        if (formato == BarcodeFormat.EAN_13) {
            ancho = Math.max(ancho, 90);
        }
        BitMatrix matriz = new MultiFormatWriter().encode(texto, formato, ancho, alto);

        BufferedImage imagen = new BufferedImage(matriz.getWidth(), matriz.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matriz.getWidth(); x++) {
            for (int y = 0; y < matriz.getHeight(); y++) {
                imagen.setRGB(x, y, matriz.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return imagen;
    }

    /** Contenido binario PNG del codigo de barras (para insertarlo en PDFs). */
    public byte[] generarPng(String codigo) throws Exception {
        BufferedImage imagen = generarImagen(codigo);
        ByteArrayOutputStream flujo = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", flujo);
        return flujo.toByteArray();
    }

    /**
     * Guarda la imagen en {@code data/codigos_barras/<codigo>.png}
     * y devuelve la ruta.
     */
    public Path guardarPng(String codigo) throws Exception {
        Path carpeta = AppContexto.config().getDirectorioDatos().resolve(CARPETA_IMAGENES);
        Files.createDirectories(carpeta);
        Path destino = carpeta.resolve(codigo.replaceAll("[^A-Za-z0-9._-]", "_") + ".png");
        ImageIO.write(generarImagen(codigo), "png", destino.toFile());
        return destino;
    }

    // ================================================================== //
    //                     GENERACION DE CODIGOS                          //
    // ================================================================== //

    /**
     * Siguiente codigo EAN-13 libre para un producto nuevo.
     * Se usa el prefijo 84 (España) y se incrementa la serie 84xxxxxxxxxx.
     */
    public String siguienteCodigo() {
        long maximoBase = 840000000000L - 1L;
        for (var producto : FactoriaRepositorios.productos().listarTodos()) {
            String codigo = producto.getCodigoBarras();
            if (codigo == null || codigo.length() != 13 || !codigo.startsWith("84")
                    || !codigo.chars().allMatch(Character::isDigit)) {
                continue;
            }
            try {
                long base = Long.parseLong(codigo.substring(0, 12));
                maximoBase = Math.max(maximoBase, base);
            } catch (NumberFormatException ignored) {
                // no es numerico: se ignora
            }
        }
        long siguiente = maximoBase + 1;
        if (siguiente > 849999999999L) {
            siguiente = 840000000000L;
        }
        return generarEan13(siguiente);
    }

    /** Convierte 12 digitos en un EAN-13 completo (añade el digito de control). */
    public static String generarEan13(long doceDigitos) {
        String base = String.format("%012d", doceDigitos);
        return base + digitoControl(base);
    }

    /** Digito de control EAN/UPC (modulo 10, pesos 1-3). */
    public static int digitoControl(String doceDigitos) {
        int suma = 0;
        for (int i = 0; i < 12 && i < doceDigitos.length(); i++) {
            int digito = doceDigitos.charAt(i) - '0';
            suma += (i % 2 == 0) ? digito : digito * 3;
        }
        return (10 - (suma % 10)) % 10;
    }

    /** true si el texto es un EAN-13 con digito de control correcto. */
    public static boolean esEan13Valido(String codigo) {
        if (codigo == null || codigo.length() != 13) {
            return false;
        }
        if (!codigo.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return digitoControl(codigo.substring(0, 12)) == (codigo.charAt(12) - '0');
    }

    /** Devuelve un mensaje de error si el codigo no es valido, o vacio si lo es. */
    public Optional<String> validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.of("El codigo de barras es obligatorio.");
        }
        String limpio = codigo.trim();
        if (limpio.length() == 13 && limpio.chars().allMatch(Character::isDigit)
                && !esEan13Valido(limpio)) {
            return Optional.of("El codigo EAN-13 no es valido: el digito de control no coincide.");
        }
        if (limpio.length() > 50) {
            return Optional.of("El codigo de barras no puede superar los 50 caracteres.");
        }
        return Optional.empty();
    }
}
