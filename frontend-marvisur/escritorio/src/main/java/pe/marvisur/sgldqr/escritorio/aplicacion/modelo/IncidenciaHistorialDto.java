package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Registro de una incidencia histórica para una encomienda.
 */
public record IncidenciaHistorialDto(
        String idIncidencia,
        String idEncomienda,
        String idEmpleadoReporta,
        String tipoIncidencia,
        String motivoReimpresion,
        String fechaIncidencia
) {
}
