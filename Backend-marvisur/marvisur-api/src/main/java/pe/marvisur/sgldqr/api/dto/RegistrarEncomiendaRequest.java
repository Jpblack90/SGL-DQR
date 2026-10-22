package pe.marvisur.sgldqr.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.marvisur.sgldqr.aplicacion.ModalidadPago;

import java.math.BigDecimal;

/**
 * Petición de registro de nueva encomienda en ventanilla (RF-04).
 */
public record RegistrarEncomiendaRequest(
        @NotNull(message = "Los datos del remitente son obligatorios")
        @Valid
        ClienteInputDto remitente,

        @NotNull(message = "Los datos del destinatario son obligatorios")
        @Valid
        ClienteInputDto destinatario,

        @NotBlank(message = "La sucursal de destino es obligatoria")
        String idSucursalDestino,

        @NotNull(message = "El peso en kilogramos es obligatorio")
        @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0 kg")
        BigDecimal pesoKg,

        @DecimalMin(value = "0.0001", message = "El volumen debe ser mayor a 0 m³")
        BigDecimal volumenM3,

        String descripcionContenido,

        @NotNull(message = "La modalidad de pago es obligatoria (CONTADO o CONTRA_ENTREGA)")
        ModalidadPago modalidadPago,

        String codigoExpress
) {
    public RegistrarEncomiendaRequest(ClienteInputDto remitente, ClienteInputDto destinatario,
                                     String idSucursalDestino, BigDecimal pesoKg,
                                     BigDecimal volumenM3, String descripcionContenido,
                                     ModalidadPago modalidadPago) {
        this(remitente, destinatario, idSucursalDestino, pesoKg, volumenM3, descripcionContenido, modalidadPago, null);
    }
}
