package pe.marvisur.sgldqr.escritorio.aplicacion.puertos;

/**
 * Puerto para monitorear el estado de conectividad con el servidor central de Marvisur.
 */
public interface EstadoConexionPort {

    /**
     * Comprueba si el servidor REST responde favorablemente (ej. /actuator/health o /api/v1/publico/ping).
     *
     * @return {@code true} si hay conexión activa con la API, {@code false} de lo contrario
     */
    boolean verificarConectividad();
}
