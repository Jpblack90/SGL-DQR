package pe.marvisur.sgldqr.api.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Encomienda no entregada sin eventos de movimiento en el sistema por encima del umbral de horas (RF-02).
 * Cero datos personales: no incluye nombres, documentos de identidad ni teléfonos.
 */
@Schema(description = "Datos operativos de una encomienda estancada sin movimiento")
public record EncomiendaEstancadaDto(
        @Schema(description = "Código visible del comprobante", example = "T001-00000042")
        String codigoComprobante,

        @Schema(description = "Estado logístico actual de la encomienda", example = "EN_ALMACEN_ORIGEN")
        String estadoLogistico,

        @Schema(description = "Nombre de la agencia de destino", example = "Agencia Arequipa")
        String nombreAgenciaDestino,

        @Schema(description = "Horas transcurridas desde el último evento o registro", example = "52")
        long horasSinMovimiento
) {
}
