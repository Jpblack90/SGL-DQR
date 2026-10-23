package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Resultado estructurado de la validación cruzada para entrega física en almacén.
 */
public record ValidacionEntregaResultado(
        TipoValidacion tipo,
        String idEncomienda,
        String codigoComprobante,
        BigDecimal montoPendiente,
        String estadoLogistico,
        long latenciaMs,
        String mensaje
) {
    public enum TipoValidacion {
        EXITOSO,
        PAGO_PENDIENTE,
        TOKEN_INVALIDO,
        TRANSICION_INVALIDA,
        NO_ENCONTRADO,
        ERROR_INESPERADO
    }

    public boolean esExitoso() {
        return tipo == TipoValidacion.EXITOSO;
    }

    public boolean esPagoPendiente() {
        return tipo == TipoValidacion.PAGO_PENDIENTE;
    }

    @Override
    public String toString() {
        return "ValidacionEntregaResultado{" +
                "tipo=" + tipo +
                ", idEncomienda='" + idEncomienda + '\'' +
                ", codigoComprobante='" + codigoComprobante + '\'' +
                ", montoPendiente=" + montoPendiente +
                ", estadoLogistico='" + estadoLogistico + '\'' +
                ", latenciaMs=" + latenciaMs +
                '}';
    }
}
