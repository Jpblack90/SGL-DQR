package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.VehiculoJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.Vehiculo;

public final class VehiculoMapper {

    private VehiculoMapper() {
    }

    public static VehiculoJpaEntity aEntidad(Vehiculo dominio) {
        if (dominio == null) return null;
        return new VehiculoJpaEntity(
                dominio.getIdVehiculo(),
                dominio.getPlaca(),
                dominio.getModelo(),
                dominio.getCapacidadCargaKg()
        );
    }

    public static Vehiculo aDominio(VehiculoJpaEntity entidad) {
        if (entidad == null) return null;
        return new Vehiculo(
                entidad.getIdVehiculo(),
                entidad.getPlaca(),
                entidad.getModelo(),
                entidad.getCapacidadCargaKg()
        );
    }
}
