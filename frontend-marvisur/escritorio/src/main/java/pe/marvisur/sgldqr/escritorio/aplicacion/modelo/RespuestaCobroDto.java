package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Respuesta devuelta tras registrar el cobro de una encomienda Contra Entrega.
 */
public record RespuestaCobroDto(
        String idEncomienda,
        String codigoComprobante,
        BigDecimal montoTotal,
        String estadoPago,
        String estadoLogistico,
        String mensaje,
        String fechaCobro
) {
}
