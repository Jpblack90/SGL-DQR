package pe.marvisur.sgldqr.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Petición pública de cotización estimada de envío (RF-10).
 */
public record CotizacionRequest(
        @NotBlank(message = "La sucursal de origen es obligatoria")
        String idSucursalOrigen,

        @NotBlank(message = "La sucursal de destino es obligatoria")
        String idSucursalDestino,

        @NotNull(message = "El peso en kilogramos es obligatorio")
        @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0 kg")
        BigDecimal pesoKg,

        BigDecimal volumenM3
) {
}
