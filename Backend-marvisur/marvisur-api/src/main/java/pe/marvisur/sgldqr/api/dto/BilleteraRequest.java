package pe.marvisur.sgldqr.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Petición para consultar la orden y llave digital en la Billetera Digital (RF-09).
 * El token nunca debe revelarse en logs ni cadenas de texto (toString protegido).
 */
public record BilleteraRequest(
        @NotBlank(message = "El token de retiro es obligatorio")
        String token
) {
    @Override
    public String toString() {
        return "BilleteraRequest[token=PROTEGIDO]";
    }
}
