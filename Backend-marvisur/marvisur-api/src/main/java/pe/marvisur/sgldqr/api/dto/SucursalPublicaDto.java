package pe.marvisur.sgldqr.api.dto;

/**
 * Representación pública de una sucursal/agencia de Expreso Marvisur para el portal web (RF-08).
 */
public record SucursalPublicaDto(
        String id,
        String nombreAgencia,
        String ciudad,
        String direccion
) {
}
