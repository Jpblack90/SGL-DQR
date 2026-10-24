package pe.marvisur.sgldqr.api.persistencia.repositorio;

import java.math.BigDecimal;

/**
 * Proyección Spring Data JPA para la recaudación agregada por sucursal de origen.
 */
public interface RecaudacionSucursalProjection {
    String getIdSucursal();
    String getNombreAgencia();
    BigDecimal getRecaudado();
    BigDecimal getPendiente();
}
