package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Respuesta devuelta tras registrar el cobro de una encomienda en modalidad Contra Entrega (RF-04).
 */
@Schema(description = "Respuesta de confirmación de cobro de encomienda Contra Entrega")
public record RespuestaCobroDto(

        @Schema(description = "Identificador único de la encomienda", example = "7b92f4e8-469b-43d7-a5c9-123456789abc")
        String idEncomienda,

        @Schema(description = "Código de comprobante oficial de la encomienda", example = "MV-20261021-3A7MLMKY")
        String codigoComprobante,

        @Schema(description = "Monto total cobrado", example = "25.50")
        BigDecimal montoTotal,

        @Schema(description = "Nuevo estado de pago de la encomienda", example = "PAGADO")
        String estadoPago,

        @Schema(description = "Estado logístico actual de la encomienda", example = "EN_ALMACEN_DESTINO")
        String estadoLogistico,

        @Schema(description = "Mensaje informativo de la operación", example = "Cobro registrado exitosamente. La encomienda ha sido desbloqueada para su validación cruzada y entrega.")
        String mensaje,

        @Schema(description = "Fecha y hora del registro del cobro", example = "2026-10-21T19:30:00")
        String fechaCobro
) {}
