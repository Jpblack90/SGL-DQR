package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para elementos del catálogo de roles en combos y selectores (RF-01, RF-03).
 */
@Schema(description = "Elemento del catálogo de roles")
public record RolComboDto(
        @Schema(description = "Identificador del rol", example = "ROL-TN1-RECEPCION")
        String id,

        @Schema(description = "Nombre legible del rol", example = "Recepcionista")
        String nombre
) {
}
