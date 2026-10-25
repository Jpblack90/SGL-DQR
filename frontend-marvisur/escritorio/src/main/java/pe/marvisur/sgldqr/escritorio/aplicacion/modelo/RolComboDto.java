package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Elemento del catálogo de roles para combos y selectores (RF-01, RF-03).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RolComboDto(
        String id,
        String nombre
) {
    @Override
    public String toString() {
        return nombre != null ? nombre : "";
    }
}
