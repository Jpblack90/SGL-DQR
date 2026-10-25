package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Petición para modificar el estado de un viaje programado (RF-05).
 */
public record CambiarEstadoViajeRequest(
        String estado
) {
}
