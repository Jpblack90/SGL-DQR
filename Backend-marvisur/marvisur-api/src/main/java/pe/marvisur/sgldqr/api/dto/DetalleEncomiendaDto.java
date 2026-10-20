package pe.marvisur.sgldqr.api.dto;

import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;

import java.math.BigDecimal;

/**
 * DTO para la consulta y listado de encomiendas.
 */
public record DetalleEncomiendaDto(
        String idEncomienda,
        String codigoComprobante,
        String idRemitente,
        String idDestinatario,
        String idSucursalOrigen,
        String idSucursalDestino,
        BigDecimal pesoKg,
        BigDecimal volumenM3,
        String descripcionContenido,
        BigDecimal montoTotal,
        EstadoPago estadoPago,
        EstadoLogistico estadoLogistico,
        String fechaRegistro
) {
}
