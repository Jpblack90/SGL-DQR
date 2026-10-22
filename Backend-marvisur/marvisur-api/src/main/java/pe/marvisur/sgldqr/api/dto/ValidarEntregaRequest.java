package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Petición de validación cruzada del Doble QR para entrega física en almacén de destino (RF-07).
 *
 * NOTA DE SEGURIDAD:
 * Se sobreescribe {@link #toString()} para enmascarar el valor de {@code qr2} (token en claro),
 * garantizando que jamás se registre en logs de depuración ni trazas de error (RNF-01).
 */
@Schema(description = "Petición de validación cruzada Doble QR para entrega física")
public record ValidarEntregaRequest(

        @NotBlank(message = "El código QR1 físico es obligatorio")
        @Schema(description = "Código escaneado del QR1 impreso en el bulto físico", example = "MARVISUR:QR1:7b92f4e8-469b-43d7-a5c9-123456789abc")
        String qr1,

        @NotBlank(message = "El código QR2 digital es obligatorio")
        @Schema(description = "Token digital QR2 presentado por el cliente desde su billetera web", example = "a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef0")
        String qr2
) {

    @Override
    public String toString() {
        return "ValidarEntregaRequest[qr1=" + qr1 + ", qr2=***]";
    }
}
