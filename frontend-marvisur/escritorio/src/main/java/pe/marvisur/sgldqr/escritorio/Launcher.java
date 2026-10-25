package pe.marvisur.sgldqr.escritorio;

/**
 * Punto de entrada alternativo para empaquetado y arranque standalone (jpackage / JAR).
 * <p>
 * No extiende {@link javafx.application.Application} para evitar que el launcher de Java
 * requiera módulos de JavaFX en el module-path al ejecutar en entornos no modulares.
 * </p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
