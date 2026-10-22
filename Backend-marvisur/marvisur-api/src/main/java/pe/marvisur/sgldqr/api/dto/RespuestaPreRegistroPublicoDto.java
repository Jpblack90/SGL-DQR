package pe.marvisur.sgldqr.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta pública tras crear un Pre-Registro Web exitoso (RF-10).
 */
public record RespuestaPreRegistroPublicoDto(
        String codigoExpress,
        LocalDateTime fechaExpiracion,
        BigDecimal montoCotizado,
        BigDecimal pesoEstimado,
        String sucursalDestino,
        String remitenteEnmascarado,
        String destinatarioEnmascarado
) {
}
