package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Recaudación y montos pendientes agrupados por sucursal de origen (RF-02).
 */
public record RecaudacionSucursalDto(
        String idSucursal,
        String nombreAgencia,
        BigDecimal recaudado,
        BigDecimal pendiente
) {
}
