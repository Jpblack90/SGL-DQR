package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.HistorialTrackingJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.HistorialTracking;

/**
 * Mapeador manual entre HistorialTracking y HistorialTrackingJpaEntity.
 */
public final class HistorialTrackingMapper {

    private HistorialTrackingMapper() {
    }

    public static HistorialTracking aDominio(HistorialTrackingJpaEntity entidad) {
        if (entidad == null) return null;

        return new HistorialTracking(
                entidad.getIdTracking(),
                entidad.getIdEncomienda(),
                entidad.getIdSucursalActual(),
                entidad.getEstadoEvento(),
                entidad.getObservacion(),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaHoraEvento())
        );
    }

    public static HistorialTrackingJpaEntity aEntidad(HistorialTracking dominio) {
        if (dominio == null) return null;

        return new HistorialTrackingJpaEntity(
                dominio.getIdTracking(),
                dominio.getIdEncomienda(),
                dominio.getIdSucursalActual(),
                dominio.getEstadoEvento(),
                dominio.getObservacion(),
                FechaMapperHelper.aOffsetDateTime(dominio.getFechaHoraEvento())
        );
    }
}
