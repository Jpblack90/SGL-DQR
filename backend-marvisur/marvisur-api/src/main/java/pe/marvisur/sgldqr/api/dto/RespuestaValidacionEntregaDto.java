package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta devuelta tras una validación cruzada y entrega física exitosa (RF-07).
 */
@Schema(description = "Respuesta de validación cruzada y entrega física exitosa")
public record RespuestaValidacionEntregaDto(

        @Schema(description = "Identificador único de la encomienda entregada", example = "7b92f4e8-469b-43d7-a5c9-123456789abc")
        String idEncomienda,

        @Schema(description = "Código de comprobante oficial de la encomienda", example = "MV-20261021-3A7MLMKY")
        String codigoComprobante,

        @Schema(description = "Resultado oficial de la validación cruzada", example = "EXITOSO")
        String resultado,

        @Schema(description = "Estado logístico final tras la entrega consumada", example = "ENTREGADO")
        String estadoLogistico,

        @Schema(description = "Mensaje descriptivo del resultado de la operación", example = "Entrega física autorizada y token QR2 invalidado de forma irreversible.")
        String mensaje,

        @Schema(description = "Latencia del procesamiento criptográfico en milisegundos (RNF-03)", example = "15")
        long latenciaMs,

        @Schema(description = "Fecha y hora en que se consumó la entrega física", example = "2026-10-21T19:30:00")
        String fechaEntrega
) {}
