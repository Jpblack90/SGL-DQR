package pe.marvisur.sgldqr.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Petición pública para pre-registrar un envío desde el portal web ('Código Express', RF-10).
 */
public record CrearPreRegistroPublicoRequest(
        @NotNull(message = "Los datos del remitente son obligatorios")
        @Valid
        ClienteInputDto remitente,

        @NotNull(message = "Los datos del destinatario son obligatorios")
        @Valid
        ClienteInputDto destinatario,

        @NotBlank(message = "La sucursal de destino es obligatoria")
        String idSucursalDestino,

        @NotNull(message = "El peso estimado es obligatorio")
        @DecimalMin(value = "0.01", message = "El peso estimado debe ser mayor a 0 kg")
        BigDecimal pesoEstimado
) {
}
