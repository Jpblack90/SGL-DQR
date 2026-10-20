package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;

/**
 * Mapeador manual entre la entidad de dominio Encomienda y la entidad JPA EncomiendaJpaEntity.
 */
public final class EncomiendaMapper {

    private EncomiendaMapper() {
    }

    public static Encomienda aDominio(EncomiendaJpaEntity entidad) {
        if (entidad == null) return null;

        return new Encomienda(
                entidad.getIdEncomienda(),
                entidad.getCodigoComprobante(),
                entidad.getIdRemitente(),
                entidad.getIdDestinatario(),
                entidad.getIdSucursalOrigen(),
                entidad.getIdSucursalDestino(),
                entidad.getPesoKg(),
                entidad.getVolumenM3(),
                entidad.getDescripcionContenido(),
                entidad.getMontoTotal(),
                EstadoPago.valueOf(entidad.getEstadoPago()),
                EstadoLogistico.valueOf(entidad.getEstadoLogistico()),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaRegistro())
        );
    }

    public static EncomiendaJpaEntity aEntidad(Encomienda dominio) {
        if (dominio == null) return null;

        return new EncomiendaJpaEntity(
                dominio.getIdEncomienda(),
                dominio.getCodigoComprobante(),
                dominio.getIdRemitente(),
                dominio.getIdDestinatario(),
                dominio.getIdSucursalOrigen(),
                dominio.getIdSucursalDestino(),
                dominio.getPesoKg(),
                dominio.getVolumenM3(),
                dominio.getDescripcionContenido(),
                dominio.getMontoTotal(),
                dominio.getEstadoPago().name(),
                dominio.getEstadoLogistico().name(),
                FechaMapperHelper.aOffsetDateTime(dominio.getFechaRegistro())
        );
    }
}
