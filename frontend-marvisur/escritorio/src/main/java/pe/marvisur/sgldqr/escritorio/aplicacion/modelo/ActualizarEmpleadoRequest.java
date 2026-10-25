package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Petición de actualización de datos de un empleado (RF-03).
 * Mapeo exacto con los campos requeridos por la API central.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ActualizarEmpleadoRequest(
        String nombres,
        String apellidos,
        String correo,
        String idRol,
        String idSucursal
) {
}
