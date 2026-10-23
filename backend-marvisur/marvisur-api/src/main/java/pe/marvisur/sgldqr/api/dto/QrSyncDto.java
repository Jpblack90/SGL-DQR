package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos del par criptográfico Doble QR generados offline por el cliente.
 * El QR2 viaja exclusivamente como hash criptográfico SHA-256 (64 hex).
 */
@Schema(description = "Identificadores y hashes del Doble QR generados en el dispositivo offline")
public record QrSyncDto(

        @Schema(description = "Identificador único de la pareja Doble QR (UUID)", example = "7b92f4e8-469b-43d7-a5c9-123456789abc")
        @NotBlank(message = "El idPareja es obligatorio")
        String idPareja,

        @Schema(description = "Identificador operativo del QR1 físico para el bulto", example = "MARVISUR:QR1:7b92f4e8-469b-43d7-a5c9-123456789abc")
        @NotBlank(message = "El uuidQr1 es obligatorio")
        String uuidQr1,

        @Schema(description = "Hash SHA-256 (64 caracteres hexadecimales) de la llave digital QR2", example = "a1b2c3d4e5f60718293a4b5c6d7e8f90123456789abcdef0123456789abcdef0")
        @NotBlank(message = "El hashQr2 es obligatorio")
        @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "El hashQr2 debe ser una cadena hexadecimal SHA-256 de 64 caracteres")
        String hashQr2
) {
        @Override
        public String toString() {
                // Protección estricta: nunca imprimir el hash completo en logs
                return "QrSyncDto[idPareja=" + idPareja + ", uuidQr1=" + uuidQr1 + ", hashQr2=***PROTECTED***]";
        }
}
