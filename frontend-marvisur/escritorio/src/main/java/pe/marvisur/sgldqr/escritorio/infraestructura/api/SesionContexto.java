package pe.marvisur.sgldqr.escritorio.infraestructura.api;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.UsuarioSesion;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Contenedor de estado de sesión en memoria para la aplicación de escritorio.
 * <p>
 * REGLA DE SEGURIDAD: El token JWT reside única y exclusivamente en memoria RAM.
 * Nunca se almacena en disco, SQLite, SharedPreferences ni variables persistentes.
 * </p>
 */
public final class SesionContexto {

    private static final AtomicReference<UsuarioSesion> SESION_ACTUAL = new AtomicReference<>(null);
    private static final AtomicReference<Consumer<String>> LISTENER_EXPIRACION = new AtomicReference<>(null);

    private SesionContexto() {
    }

    public static void establecerSesion(UsuarioSesion sesion) {
        SESION_ACTUAL.set(sesion);
    }

    public static UsuarioSesion obtenerSesion() {
        return SESION_ACTUAL.get();
    }

    public static String obtenerToken() {
        UsuarioSesion sesion = SESION_ACTUAL.get();
        return sesion != null ? sesion.token() : null;
    }

    public static boolean estaAutenticado() {
        UsuarioSesion sesion = SESION_ACTUAL.get();
        return sesion != null && sesion.token() != null && !sesion.token().isBlank();
    }

    public static void limpiar() {
        SESION_ACTUAL.set(null);
    }

    public static void registrarListenerExpiracion(Consumer<String> listener) {
        LISTENER_EXPIRACION.set(listener);
    }

    public static void notificarSesionExpirada(String motivo) {
        limpiar();
        Consumer<String> listener = LISTENER_EXPIRACION.get();
        if (listener != null) {
            listener.accept(motivo != null ? motivo : "Sesión expirada");
        }
    }
}
