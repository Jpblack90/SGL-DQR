package pe.marvisur.sgldqr.api.dto;

import java.math.BigDecimal;

/**
 * Respuesta para la Billetera Digital del cliente (RF-09).
 */
public record RespuestaBilleteraDto(
        String codigoComprobante,
        String estadoLogistico,
        String estadoPago,
        BigDecimal montoTotal,
        String agenciaDestino,
        String ciudadDestino,
        String estadoToken,
        String qr2Base64
) {
}
