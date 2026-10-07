package com.inventario.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Hash de contrasenas con SHA-256 + sal aleatoria.
 * Formato almacenado: {@code <salHex>:<hashHex>}
 */
public final class UtilHash {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private UtilHash() {
    }

    /** Crea un hash nuevo con sal aleatoria. */
    public static String crear(String contrasena) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        String salHex = hex(sal);
        return salHex + ":" + sha256(salHex, contrasena);
    }

    /** Comprueba una contrasena contra el hash almacenado. */
    public static boolean verificar(String contrasena, String almacenado) {
        if (almacenado == null || almacenado.isBlank() || contrasena == null) {
            return false;
        }
        String[] partes = almacenado.split(":");
        if (partes.length != 2) {
            // Sin formato correcto: se compara tal cual (compatibilidad).
            return almacenado.equals(contrasena);
        }
        String esperado = sha256(partes[0], contrasena);
        return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                partes[1].getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(String sal, String contrasena) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] resultado = digest.digest((sal + contrasena).getBytes(StandardCharsets.UTF_8));
            return hex(resultado);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
