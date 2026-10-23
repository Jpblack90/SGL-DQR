package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Parámetros para reportar una incidencia de etiqueta dañada o perdida.
 */
public record RegistrarIncidenciaComando(
        String idEncomienda,
        String tipoIncidencia,
        String motivo
) {
}
