package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.IncidenciaEmergenciaJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.IncidenciaEmergencia;
import pe.marvisur.sgldqr.dominio.modelo.TipoIncidencia;

public final class IncidenciaEmergenciaMapper {

    private IncidenciaEmergenciaMapper() {
    }

    public static IncidenciaEmergenciaJpaEntity aEntidad(IncidenciaEmergencia dominio) {
        if (dominio == null) return null;
        return new IncidenciaEmergenciaJpaEntity(
                dominio.getIdIncidencia(),
                dominio.getIdEncomienda(),
                dominio.getIdEmpleadoReporta(),
                dominio.getTipoIncidencia().name(),
                dominio.getMotivoReimpresion(),
                FechaMapperHelper.aOffsetDateTime(dominio.getFechaIncidencia())
        );
    }

    public static IncidenciaEmergencia aDominio(IncidenciaEmergenciaJpaEntity entidad) {
        if (entidad == null) return null;
        return new IncidenciaEmergencia(
                entidad.getIdIncidencia(),
                entidad.getIdEncomienda(),
                entidad.getIdEmpleadoReporta(),
                TipoIncidencia.valueOf(entidad.getTipoIncidencia()),
                entidad.getMotivoReimpresion(),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaIncidencia())
        );
    }
}
