package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.Empleado;

import java.util.Optional;

/**
 * Puerto de repositorio para la entidad Empleado.
 */
public interface RepositorioEmpleadoPort {

    void guardar(Empleado empleado);

    Optional<Empleado> buscarPorId(String idEmpleado);

    Optional<Empleado> buscarPorDni(String dni);

    Optional<Empleado> buscarPorCorreo(String correo);
}
