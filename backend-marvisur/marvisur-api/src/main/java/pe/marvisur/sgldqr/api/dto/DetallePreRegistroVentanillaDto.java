package pe.marvisur.sgldqr.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos completos del pre-registro express para precargar la atención en ventanilla (TN1, TN3).
 */
public record DetallePreRegistroVentanillaDto(
        String idPreRegistro,
        String codigoExpress,
        ClienteInputDto remitente,
        ClienteInputDto destinatario,
        String idSucursalDestino,
        BigDecimal pesoEstimado,
        BigDecimal montoCotizado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaExpiracion,
        String estado
) {
}
