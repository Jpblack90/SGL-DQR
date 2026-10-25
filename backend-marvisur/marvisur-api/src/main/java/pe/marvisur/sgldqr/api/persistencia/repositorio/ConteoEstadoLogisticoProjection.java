package pe.marvisur.sgldqr.api.persistencia.repositorio;

/**
 * Proyección Spring Data JPA para el conteo de encomiendas por estado logístico.
 */
public interface ConteoEstadoLogisticoProjection {
    String getEstadoLogistico();
    Long getTotal();
}
