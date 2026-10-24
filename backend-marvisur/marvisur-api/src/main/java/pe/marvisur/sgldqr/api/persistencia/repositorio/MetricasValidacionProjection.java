package pe.marvisur.sgldqr.api.persistencia.repositorio;

/**
 * Proyección Spring Data JPA para las métricas de validación cruzada Doble QR.
 */
public interface MetricasValidacionProjection {
    Long getTotal();
    Long getExitosas();
    Long getRechazadasPagoPendiente();
    Long getRechazadasTokenInvalido();
    Double getLatenciaPromedioMs();
    Integer getLatenciaMaximaMs();
}
