package pe.marvisur.sgldqr.api.persistencia.repositorio;

import java.time.OffsetDateTime;

/**
 * Proyección Spring Data JPA para la consulta de encomiendas estancadas sin movimiento.
 */
public interface EncomiendaEstancadaProjection {
    String getCodigoComprobante();
    String getEstadoLogistico();
    String getNombreAgenciaDestino();
    OffsetDateTime getUltimoMovimiento();
}
