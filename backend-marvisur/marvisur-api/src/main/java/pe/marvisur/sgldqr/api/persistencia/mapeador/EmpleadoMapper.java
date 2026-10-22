package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.Empleado;

/**
 * Mapeador manual entre Empleado y EmpleadoJpaEntity.
 */
public final class EmpleadoMapper {

    private EmpleadoMapper() {
    }

    public static Empleado aDominio(EmpleadoJpaEntity entidad) {
        if (entidad == null) return null;

        return new Empleado(
                entidad.getIdEmpleado(),
                entidad.getIdRol(),
                entidad.getIdSucursal(),
                entidad.getDni(),
                entidad.getNombres(),
                entidad.getApellidos(),
                entidad.getCorreo(),
                entidad.getPasswordHash(),
                Boolean.TRUE.equals(entidad.getEstado())
        );
    }

    public static EmpleadoJpaEntity aEntidad(Empleado dominio) {
        if (dominio == null) return null;

        return new EmpleadoJpaEntity(
                dominio.getIdEmpleado(),
                dominio.getIdRol(),
                dominio.getIdSucursal(),
                dominio.getDni(),
                dominio.getNombres(),
                dominio.getApellidos(),
                dominio.getCorreo(),
                dominio.getPasswordHash(),
                dominio.isEstado()
        );
    }
}
