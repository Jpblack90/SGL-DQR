package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.RutaJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.Ruta;

public final class RutaMapper {

    private RutaMapper() {
    }

    public static RutaJpaEntity aEntidad(Ruta dominio) {
        if (dominio == null) return null;
        return new RutaJpaEntity(
                dominio.getIdRuta(),
                dominio.getIdSucursalOrigen(),
                dominio.getIdSucursalDestino(),
                dominio.getDistanciaKm()
        );
    }

    public static Ruta aDominio(RutaJpaEntity entidad) {
        if (entidad == null) return null;
        return new Ruta(
                entidad.getIdRuta(),
                entidad.getIdSucursalOrigen(),
                entidad.getIdSucursalDestino(),
                entidad.getDistanciaKm()
        );
    }
}
