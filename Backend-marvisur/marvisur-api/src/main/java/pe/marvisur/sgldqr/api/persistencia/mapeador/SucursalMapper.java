package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.Sucursal;

/**
 * Mapeador manual entre Sucursal y SucursalJpaEntity.
 */
public final class SucursalMapper {

    private SucursalMapper() {
    }

    public static Sucursal aDominio(SucursalJpaEntity entidad) {
        if (entidad == null) return null;

        return new Sucursal(
                entidad.getIdSucursal(),
                entidad.getNombreAgencia(),
                entidad.getDireccion(),
                entidad.getCiudad()
        );
    }

    public static SucursalJpaEntity aEntidad(Sucursal dominio) {
        if (dominio == null) return null;

        return new SucursalJpaEntity(
                dominio.getIdSucursal(),
                dominio.getNombreAgencia(),
                dominio.getDireccion(),
                dominio.getCiudad()
        );
    }
}
