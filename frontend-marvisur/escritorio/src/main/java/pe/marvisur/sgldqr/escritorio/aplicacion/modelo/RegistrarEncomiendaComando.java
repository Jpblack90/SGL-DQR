package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Parámetros para registrar una nueva encomienda en ventanilla.
 */
public record RegistrarEncomiendaComando(
        ClienteDto remitente,
        ClienteDto destinatario,
        String idSucursalDestino,
        BigDecimal pesoKg,
        BigDecimal volumenM3,
        String descripcionContenido,
        String modalidadPago
) {
}
