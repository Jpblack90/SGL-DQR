package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Métricas de validación cruzada Doble QR realizadas durante el período (RF-02, RF-04, RNF-03).
 */
public record ValidacionesDashboardDto(
        long total,
        long exitosas,
        long rechazadasPagoPendiente,
        long rechazadasTokenInvalido,
        Double latenciaPromedioMs,
        Integer latenciaMaximaMs
) {
}
