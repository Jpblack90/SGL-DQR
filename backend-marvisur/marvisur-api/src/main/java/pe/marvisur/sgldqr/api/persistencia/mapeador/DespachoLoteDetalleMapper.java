package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.DespachoLoteDetalleJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.DespachoLoteDetalle;

public final class DespachoLoteDetalleMapper {

    private DespachoLoteDetalleMapper() {
    }

    public static DespachoLoteDetalleJpaEntity aEntidad(DespachoLoteDetalle dominio) {
        if (dominio == null) return null;
        return new DespachoLoteDetalleJpaEntity(
                dominio.getIdDespachoDetalle(),
                dominio.getIdViaje(),
                dominio.getIdEncomienda(),
                FechaMapperHelper.aOffsetDateTime(dominio.getFechaAsociacion())
        );
    }

    public static DespachoLoteDetalle aDominio(DespachoLoteDetalleJpaEntity entidad) {
        if (entidad == null) return null;
        return new DespachoLoteDetalle(
                entidad.getIdDespachoDetalle(),
                entidad.getIdViaje(),
                entidad.getIdEncomienda(),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaAsociacion())
        );
    }
}
