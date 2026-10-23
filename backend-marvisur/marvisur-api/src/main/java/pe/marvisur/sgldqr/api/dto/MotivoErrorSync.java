package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Motivos tipificados de rechazo durante la sincronización offline.
 */
@Schema(description = "Motivo de rechazo al sincronizar una encomienda")
public enum MotivoErrorSync {
    @Schema(description = "Error de formato o campos obligatorios inválidos")
    VALIDACION,

    @Schema(description = "La sucursal de origen no coincide con la del colaborador autenticado")
    SUCURSAL_NO_CORRESPONDE,

    @Schema(description = "El código de comprobante ya está asignado a otra encomienda distinta")
    CODIGO_COMPROBANTE_DUPLICADO,

    @Schema(description = "Una o ambas sucursales especificadas no existen en la base de datos")
    SUCURSAL_INEXISTENTE,

    @Schema(description = "Error interno no esperado durante el procesamiento del ítem")
    ERROR_INTERNO
}
