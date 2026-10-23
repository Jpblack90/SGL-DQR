package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado del resultado del procesamiento de sincronización individual de una encomienda.
 */
@Schema(description = "Estado de procesamiento de sincronización del ítem")
public enum ResultadoSync {
    @Schema(description = "La encomienda fue sincronizada y persistida exitosamente")
    OK,

    @Schema(description = "La encomienda con este ID ya existía en el servidor (idempotente)")
    YA_SINCRONIZADA,

    @Schema(description = "El ítem fue rechazado por un motivo tipificado")
    ERROR
}
