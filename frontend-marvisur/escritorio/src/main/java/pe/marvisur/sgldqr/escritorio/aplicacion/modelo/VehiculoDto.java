package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Datos de un vehículo registrado en la flota de transporte (RF-05).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VehiculoDto(
        String idVehiculo,
        String placa,
        String modelo,
        BigDecimal capacidadCargaKg
) {
    public String formatoCombo() {
        return (placa != null ? placa : "") + " · " + (modelo != null ? modelo : "");
    }

    @Override
    public String toString() {
        return formatoCombo();
    }
}
