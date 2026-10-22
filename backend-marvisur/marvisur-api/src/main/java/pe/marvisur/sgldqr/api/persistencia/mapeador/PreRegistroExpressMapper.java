package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.PreRegistroExpressJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPreRegistro;
import pe.marvisur.sgldqr.dominio.modelo.PreRegistroExpress;

/**
 * Mapeador manual entre la entidad de dominio PreRegistroExpress y la entidad JPA PreRegistroExpressJpaEntity.
 */
public final class PreRegistroExpressMapper {

    private PreRegistroExpressMapper() {
    }

    public static PreRegistroExpress aDominio(PreRegistroExpressJpaEntity entidad) {
        if (entidad == null) return null;

        return new PreRegistroExpress(
                entidad.getIdPreRegistro(),
                entidad.getCodigoExpress(),
                entidad.getIdClienteRemitente(),
                entidad.getIdClienteDestinatario(),
                entidad.getIdSucursalDestino(),
                entidad.getPesoEstimado(),
                entidad.getMontoCotizado(),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaCreacion()),
                EstadoPreRegistro.valueOf(entidad.getEstado())
        );
    }

    public static PreRegistroExpressJpaEntity aEntidad(PreRegistroExpress dominio) {
        if (dominio == null) return null;

        return new PreRegistroExpressJpaEntity(
                dominio.getIdPreRegistro(),
                dominio.getCodigoExpress(),
                dominio.getIdClienteRemitente(),
                dominio.getIdClienteDestinatario(),
                dominio.getIdSucursalDestino(),
                dominio.getPesoEstimado(),
                dominio.getMontoCotizado(),
                FechaMapperHelper.aOffsetDateTime(dominio.getFechaCreacion()),
                dominio.getEstado().name()
        );
    }
}
