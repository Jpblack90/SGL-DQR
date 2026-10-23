package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Resumen de encomienda devuelto por la búsqueda de emergencia por DNI o RUC.
 */
public record EncomiendaBusquedaDto(
        String idEncomienda,
        String codigoComprobante,
        String numeroDocumentoRemitente,
        String nombreRemitente,
        String numeroDocumentoDestinatario,
        String nombreDestinatario,
        String idSucursalOrigen,
        String idSucursalDestino,
        BigDecimal pesoKg,
        BigDecimal montoTotal,
        String estadoPago,
        String estadoLogistico,
        String fechaRegistro
) {
    public boolean esPagoPendiente() {
        return "PENDIENTE".equalsIgnoreCase(estadoPago);
    }
}
