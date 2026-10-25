package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Petición para registrar un nuevo vehículo en la flota (TN3 Administrador).
 */
@Schema(description = "Petición para registrar un vehículo en la flota")
public record CrearVehiculoRequest(

        @Schema(description = "Placa de rodaje del vehículo (5 a 10 caracteres en mayúsculas, sin espacios)", example = "V1A-852")
        @NotBlank(message = "La placa es obligatoria")
        @Size(min = 5, max = 10, message = "La placa debe tener entre 5 y 10 caracteres")
        @Pattern(regexp = "^[A-Z0-9-]{5,10}$", message = "La placa debe contener entre 5 y 10 caracteres en mayúsculas sin espacios")
        String placa,

        @Schema(description = "Modelo o descripción del vehículo", example = "Volvo FH16")
        @NotBlank(message = "El modelo es obligatorio")
        @Size(max = 100, message = "El modelo no puede exceder 100 caracteres")
        String modelo,

        @Schema(description = "Capacidad de carga útil en kilogramos (debe ser mayor a cero)", example = "25000.00")
        @NotNull(message = "La capacidad de carga es obligatoria")
        @DecimalMin(value = "0.01", message = "La capacidad de carga debe ser mayor a cero")
        BigDecimal capacidadCargaKg
) {
}
