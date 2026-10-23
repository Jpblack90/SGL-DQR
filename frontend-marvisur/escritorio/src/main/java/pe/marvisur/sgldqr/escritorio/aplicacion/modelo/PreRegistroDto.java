package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Datos precargados de un pre-registro express para ventanilla.
 */
public record PreRegistroDto(
        String idPreRegistro,
        String codigoExpress,
        ClienteDto remitente,
        ClienteDto destinatario,
        String idSucursalDestino,
        BigDecimal pesoEstimado,
        BigDecimal montoCotizado,
        String fechaCreacion
) {
}
