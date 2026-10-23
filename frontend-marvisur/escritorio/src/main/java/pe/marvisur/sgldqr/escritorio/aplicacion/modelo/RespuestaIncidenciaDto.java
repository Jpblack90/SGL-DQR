package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Respuesta devuelta tras reportar una incidencia, con el QR1 operativo listo para reimpresión.
 */
public record RespuestaIncidenciaDto(
        String idIncidencia,
        String idEncomienda,
        String idEmpleadoReporta,
        String tipoIncidencia,
        String motivoReimpresion,
        String fechaIncidencia,
        String qr1Base64
) {
}
