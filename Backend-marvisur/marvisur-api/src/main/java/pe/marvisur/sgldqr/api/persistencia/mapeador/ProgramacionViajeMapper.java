package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.ProgramacionViajeJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;
import pe.marvisur.sgldqr.dominio.modelo.ProgramacionViaje;

public final class ProgramacionViajeMapper {

    private ProgramacionViajeMapper() {
    }

    public static ProgramacionViajeJpaEntity aEntidad(ProgramacionViaje dominio) {
        if (dominio == null) return null;
        return new ProgramacionViajeJpaEntity(
                dominio.getIdViaje(),
                dominio.getIdRuta(),
                dominio.getIdVehiculo(),
                dominio.getIdChofer(),
                FechaMapperHelper.aOffsetDateTime(dominio.getFechaSalida()),
                FechaMapperHelper.aOffsetDateTime(dominio.getFechaLlegadaEstimada()),
                dominio.getEstadoViaje().name()
        );
    }

    public static ProgramacionViaje aDominio(ProgramacionViajeJpaEntity entidad) {
        if (entidad == null) return null;
        return new ProgramacionViaje(
                entidad.getIdViaje(),
                entidad.getIdRuta(),
                entidad.getIdVehiculo(),
                entidad.getIdChofer(),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaSalida()),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaLlegadaEstimada()),
                EstadoViaje.valueOf(entidad.getEstadoViaje())
        );
    }
}
