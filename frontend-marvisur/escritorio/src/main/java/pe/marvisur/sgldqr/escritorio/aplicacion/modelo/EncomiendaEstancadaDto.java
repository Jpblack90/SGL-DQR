package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Encomienda no entregada sin eventos de movimiento en el sistema por encima del umbral de horas (RF-02).
 * Cero datos personales: no incluye nombres, documentos de identidad ni teléfonos.
 */
public record EncomiendaEstancadaDto(
        String codigoComprobante,
        String estadoLogistico,
        String nombreAgenciaDestino,
        long horasSinMovimiento
) {
}
