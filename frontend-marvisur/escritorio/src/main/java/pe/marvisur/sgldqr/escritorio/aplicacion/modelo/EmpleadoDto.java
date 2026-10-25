package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Representación inmutable de un empleado u operador logístico (RF-03).
 * Mapeo exacto con los campos devueltos por la API central.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EmpleadoDto(
        String idEmpleado,
        String dni,
        String nombres,
        String apellidos,
        String correo,
        String idRol,
        String nombreRol,
        String idSucursal,
        String nombreSucursal,
        Boolean estado
) {
    public String nombreCompleto() {
        return (nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "").trim();
    }

    public boolean esActivo() {
        return Boolean.TRUE.equals(estado);
    }

    @Override
    public String toString() {
        if (idEmpleado == null || idEmpleado.isBlank()) {
            return "Todos los colaboradores";
        }
        String nombre = nombreCompleto().trim();
        if (dni != null && !dni.isBlank()) {
            return nombre + " (" + dni.trim() + ")";
        }
        return nombre;
    }
}
