package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.AuditoriaLog;

/**
 * Mapeador manual entre AuditoriaLog y AuditoriaLogJpaEntity.
 */
public final class AuditoriaMapper {

    private AuditoriaMapper() {
    }

    public static AuditoriaLog aDominio(AuditoriaLogJpaEntity entidad) {
        if (entidad == null) return null;

        return new AuditoriaLog(
                entidad.getIdLog(),
                entidad.getIdEmpleado(),
                entidad.getAccion(),
                entidad.getTablaAfectada(),
                entidad.getIdRegistroAfectado(),
                entidad.getIpOrigen(),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaHora())
        );
    }

    public static AuditoriaLogJpaEntity aEntidad(AuditoriaLog dominio) {
        if (dominio == null) return null;

        return new AuditoriaLogJpaEntity(
                dominio.getIdLog(),
                dominio.getIdEmpleado(),
                dominio.getAccion(),
                dominio.getTablaAfectada(),
                dominio.getIdRegistroAfectado(),
                dominio.getIpOrigen(),
                FechaMapperHelper.aOffsetDateTime(dominio.getFechaHora())
        );
    }
}
