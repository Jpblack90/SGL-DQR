package pe.marvisur.sgldqr.escritorio.aplicacion.puertos;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ActualizarEmpleadoRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CambiarEstadoEmpleadoRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CrearEmpleadoRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EmpleadoDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.PaginaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RestablecerPasswordRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RolComboDto;

import java.util.List;

/**
 * Puerto de operaciones para la gestión de empleados y personal operativo (RF-03).
 */
public interface EmpleadosPort {

    PaginaDto<EmpleadoDto> listarEmpleados(String estado, String idRol, String idSucursal, String q, int pagina, int tamano);

    EmpleadoDto obtenerPorId(String id);

    EmpleadoDto crearEmpleado(CrearEmpleadoRequest request);

    EmpleadoDto actualizarEmpleado(String id, ActualizarEmpleadoRequest request);

    EmpleadoDto cambiarEstado(String id, CambiarEstadoEmpleadoRequest request);

    void restablecerPassword(String id, RestablecerPasswordRequest request);

    List<RolComboDto> listarRoles();
}
