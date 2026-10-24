package pe.marvisur.sgldqr.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modelo de datos inmutable para la generación del comprobante de envío en PDF (RF-11).
 * <p>
 * Contiene la información requerida para el documento de control interno del sistema SGL-DQR.
 * <strong>Seguridad y privacidad:</strong> Los documentos de identidad deben estar enmascarados
 * (solo los últimos 3 dígitos visibles) y no se incluyen teléfonos ni correos electrónicos.
 * </p>
 */
public record ComprobantePdfDatos(
        String tipoComprobante,
        String serieCorrelativo,
        LocalDateTime fechaEmision,
        String agenciaOrigen,
        String agenciaDestino,
        String remitenteNombre,
        String remitenteDocumentoEnmascarado,
        String destinatarioNombre,
        String destinatarioDocumentoEnmascarado,
        BigDecimal pesoKg,
        String descripcionContenido,
        String modalidadPago,
        String estadoPago,
        BigDecimal montoSubtotal,
        BigDecimal montoIgv,
        BigDecimal montoTotal,
        byte[] qr1PngBytes
) {
    public ComprobantePdfDatos {
        Objects.requireNonNull(tipoComprobante, "El tipoComprobante no puede ser nulo");
        Objects.requireNonNull(serieCorrelativo, "La serieCorrelativo no puede ser nula");
        Objects.requireNonNull(fechaEmision, "La fechaEmision no puede ser nula");
        Objects.requireNonNull(agenciaOrigen, "La agenciaOrigen no puede ser nula");
        Objects.requireNonNull(agenciaDestino, "La agenciaDestino no puede ser nula");
        Objects.requireNonNull(remitenteNombre, "El remitenteNombre no puede ser nulo");
        Objects.requireNonNull(remitenteDocumentoEnmascarado, "El remitenteDocumentoEnmascarado no puede ser nulo");
        Objects.requireNonNull(destinatarioNombre, "El destinatarioNombre no puede ser nulo");
        Objects.requireNonNull(destinatarioDocumentoEnmascarado, "El destinatarioDocumentoEnmascarado no puede ser nulo");
        Objects.requireNonNull(pesoKg, "El pesoKg no puede ser nulo");
        Objects.requireNonNull(modalidadPago, "La modalidadPago no puede ser nula");
        Objects.requireNonNull(estadoPago, "El estadoPago no puede ser nulo");
        Objects.requireNonNull(montoSubtotal, "El montoSubtotal no puede ser nulo");
        Objects.requireNonNull(montoIgv, "El montoIgv no puede ser nulo");
        Objects.requireNonNull(montoTotal, "El montoTotal no puede ser nulo");
        Objects.requireNonNull(qr1PngBytes, "El qr1PngBytes no puede ser nulo");
    }
}
