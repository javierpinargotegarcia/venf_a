package com.inventario;

import javafx.application.Application;

/**
 * Punto de entrada para ejecutar el programa:
 *
 * <pre>
 *   java -jar inventario-mercaderia-1.0.0-all.jar
 * </pre>
 *
 * Esta clase <b>no</b> extiende {@link Application} a proposito: si la clase
 * principal hereda de {@code Application}, la maquina virtual exige que
 * JavaFX este en el module-path y rechaza el arranque desde un jar con todas
 * las dependencias en el classpath. El {@code main} de {@link Aplicacion}
 * sigue existiendo para quien prefiera {@code mvn javafx:run}.
 */
public final class Lanzador {

    private Lanzador() {
    }

    public static void main(String[] args) {
        Application.launch(Aplicacion.class, args);
    }
}
