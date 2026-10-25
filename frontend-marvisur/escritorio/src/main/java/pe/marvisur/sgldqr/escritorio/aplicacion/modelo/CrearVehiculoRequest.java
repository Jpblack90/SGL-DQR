package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Petición para registrar un nuevo vehículo en la flota (RF-05).
 */
public record CrearVehiculoRequest(
        String placa,
        String modelo,
        BigDecimal capacidadCargaKg
) {
}
