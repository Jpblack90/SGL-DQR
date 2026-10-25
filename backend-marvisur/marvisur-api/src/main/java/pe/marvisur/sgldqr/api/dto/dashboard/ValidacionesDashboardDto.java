package pe.marvisur.sgldqr.api.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Métricas de validación cruzada Doble QR realizadas durante el período (RF-02, RF-04, RNF-03).
 */
@Schema(description = "Métricas agregadas de auditoría de validaciones cruzadas Doble QR")
public record ValidacionesDashboardDto(
        @Schema(description = "Total de validaciones cruzadas ejecutadas en el período", example = "35")
        long total,

        @Schema(description = "Cantidad de validaciones exitosas con entrega autorizada", example = "33")
        long exitosas,

        @Schema(description = "Cantidad de validaciones rechazadas por pago pendiente (contra-entrega no cobrado)", example = "1")
        long rechazadasPagoPendiente,

        @Schema(description = "Cantidad de validaciones rechazadas por token QR2 inválido o inexistente", example = "1")
        long rechazadasTokenInvalido,

        @Schema(description = "Latencia promedio de procesamiento en milisegundos (RNF-03)", example = "142.5")
        Double latenciaPromedioMs,

        @Schema(description = "Latencia máxima registrada en milisegundos", example = "310")
        Integer latenciaMaximaMs
) {
}
