package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.Cliente;
import pe.marvisur.sgldqr.dominio.modelo.TipoDocumento;

/**
 * Mapeador manual entre Cliente y ClienteJpaEntity.
 */
public final class ClienteMapper {

    private ClienteMapper() {
    }

    public static Cliente aDominio(ClienteJpaEntity entidad) {
        if (entidad == null) return null;

        return new Cliente(
                entidad.getIdCliente(),
                TipoDocumento.valueOf(entidad.getTipoDocumento()),
                entidad.getNumeroDocumento(),
                entidad.getNombresRazonSocial(),
                entidad.getTelefono(),
                entidad.getCorreo()
        );
    }

    public static ClienteJpaEntity aEntidad(Cliente dominio) {
        if (dominio == null) return null;

        return new ClienteJpaEntity(
                dominio.getIdCliente(),
                dominio.getTipoDocumento().name(),
                dominio.getNumeroDocumento(),
                dominio.getNombresRazonSocial(),
                dominio.getTelefono(),
                dominio.getCorreo()
        );
    }
}
