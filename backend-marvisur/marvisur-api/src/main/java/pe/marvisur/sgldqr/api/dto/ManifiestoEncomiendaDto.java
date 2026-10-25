package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Detalle de una encomienda en el manifiesto de carga de un viaje interprovincial (RF-05).
 * Cumple con la directiva estricta de CERO datos personales (sin nombres, documentos ni teléfonos).
 */
@Schema(description = "Detalle de una encomienda en el manifiesto de carga de un viaje")
public record ManifiestoEncomiendaDto(

        @Schema(description = "Código de comprobante único de la encomienda", example = "MV-20261021-A1B2C3D4")
        String codigoComprobante,

        @Schema(description = "Estado logístico actual de la encomienda", example = "EN_RUTA")
        String estadoLogistico,

        @Schema(description = "Nombre oficial de la agencia de destino", example = "Agencia Arequipa Terminal")
        String nombreAgenciaDestino,

        @Schema(description = "Peso en kilogramos de la encomienda", example = "12.50")
        BigDecimal pesoKg
) {
}
