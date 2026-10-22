package pe.marvisur.sgldqr.api.dto;

import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;

import java.math.BigDecimal;

/**
 * Respuesta emitida tras registrar exitosamente una encomienda con Doble QR.
 *
 * <p><strong>Seguridad:</strong>
 * {@code qr2Token} se entrega por única vez en este DTO y nunca se persiste ni se imprime en logs.
 * {@link #toString()} enmascara el token y el enlace para evitar fugas en herramientas de observabilidad.
 * </p>
 */
public record RegistrarEncomiendaResponse(
        String idEncomienda,
        String codigoComprobante,
        String idRemitente,
        String idDestinatario,
        String idSucursalOrigen,
        String idSucursalDestino,
        BigDecimal pesoKg,
        BigDecimal volumenM3,
        String descripcionContenido,
        BigDecimal montoTotal,
        EstadoPago estadoPago,
        EstadoLogistico estadoLogistico,
        String fechaRegistro,
        Qr1DataDto qr1,
        String qr2Token,
        String enlaceBilletera,
        String advertencia
) {
    @Override
    public String toString() {
        return "RegistrarEncomiendaResponse[" +
                "idEncomienda=" + idEncomienda + ", " +
                "codigoComprobante=" + codigoComprobante + ", " +
                "idRemitente=" + idRemitente + ", " +
                "idDestinatario=" + idDestinatario + ", " +
                "idSucursalOrigen=" + idSucursalOrigen + ", " +
                "idSucursalDestino=" + idSucursalDestino + ", " +
                "pesoKg=" + pesoKg + ", " +
                "montoTotal=" + montoTotal + ", " +
                "estadoPago=" + estadoPago + ", " +
                "estadoLogistico=" + estadoLogistico + ", " +
                "qr1Payload=" + (qr1 != null ? qr1.payload() : null) + ", " +
                "qr2Token=[PROTEGIDO], " +
                "enlaceBilletera=[PROTEGIDO], " +
                "advertencia=" + advertencia +
                "]";
    }
}
