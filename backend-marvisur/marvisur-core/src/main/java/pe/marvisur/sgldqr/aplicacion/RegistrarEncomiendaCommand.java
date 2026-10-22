package pe.marvisur.sgldqr.aplicacion;

import java.math.BigDecimal;

/**
 * Comando con los datos requeridos para registrar una encomienda en ventanilla.
 */
public record RegistrarEncomiendaCommand(
        String codigoComprobante,
        String idRemitente,
        String idDestinatario,
        String idSucursalOrigen,
        String idSucursalDestino,
        BigDecimal pesoKg,
        BigDecimal volumenM3,
        String descripcionContenido,
        ModalidadPago modalidadPago
) {
}
