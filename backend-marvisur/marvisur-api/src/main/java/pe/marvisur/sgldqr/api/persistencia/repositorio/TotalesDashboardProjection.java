package pe.marvisur.sgldqr.api.persistencia.repositorio;

import java.math.BigDecimal;

/**
 * Proyección Spring Data JPA para la consulta agregada de totales en el dashboard.
 */
public interface TotalesDashboardProjection {
    Long getRegistradas();
    Long getEntregadas();
    Long getEnRuta();
    Long getPendientesCantidad();
    BigDecimal getPendientesMonto();
    BigDecimal getRecaudado();
}
