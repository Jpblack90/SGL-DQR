package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Resultado devuelto por el backend tras registrar exitosamente una encomienda con Doble QR.
 * <p>
 * REGLA DE SEGURIDAD CRÍTICA: {@code qr2Token} solo vive en memoria mientras la ventana
 * modal de confirmación está abierta. En {@code toString()} se enmascara completamente
 * tanto el campo {@code qr2Token} como el fragmento sensible de {@code enlaceBilletera}
 * para evitar fugas inadvertidas en logs o archivos de diagnóstico.
 * </p>
 */
public record EncomiendaRegistradaResultado(
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
        String estadoPago,
        String estadoLogistico,
        String fechaRegistro,
        Qr1Data qr1,
        String qr2Token,
        String enlaceBilletera,
        String advertencia
) {
    @Override
    public String toString() {
        String enlaceSeguro = enlaceBilletera != null && enlaceBilletera.contains("#")
                ? enlaceBilletera.substring(0, enlaceBilletera.indexOf("#") + 1) + "***PROTECTED***"
                : enlaceBilletera;

        return "EncomiendaRegistradaResultado{" +
                "idEncomienda='" + idEncomienda + '\'' +
                ", codigoComprobante='" + codigoComprobante + '\'' +
                ", idSucursalOrigen='" + idSucursalOrigen + '\'' +
                ", idSucursalDestino='" + idSucursalDestino + '\'' +
                ", pesoKg=" + pesoKg +
                ", montoTotal=" + montoTotal +
                ", estadoPago='" + estadoPago + '\'' +
                ", estadoLogistico='" + estadoLogistico + '\'' +
                ", qr2Token='***PROTECTED***'" +
                ", enlaceBilletera='" + enlaceSeguro + '\'' +
                '}';
    }
}
