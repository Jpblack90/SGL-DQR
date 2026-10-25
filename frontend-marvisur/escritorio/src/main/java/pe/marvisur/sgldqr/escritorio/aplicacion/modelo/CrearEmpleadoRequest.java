package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Petición para el alta de un nuevo colaborador (RF-03).
 * Mapeo exacto con los campos requeridos por la API central.
 * Por seguridad, la contraseña nunca se expone en {@code toString()}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrearEmpleadoRequest(
        String dni,
        String nombres,
        String apellidos,
        String correo,
        String idRol,
        String idSucursal,
        String password
) {
    @Override
    public String toString() {
        return "CrearEmpleadoRequest[dni=" + dni +
                ", nombres=" + nombres +
                ", apellidos=" + apellidos +
                ", correo=" + correo +
                ", idRol=" + idRol +
                ", idSucursal=" + idSucursal +
                ", password=[PROTEGIDO]]";
    }
}
