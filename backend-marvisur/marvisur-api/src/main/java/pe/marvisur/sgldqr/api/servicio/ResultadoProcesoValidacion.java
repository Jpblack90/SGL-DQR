package pe.marvisur.sgldqr.api.servicio;

import pe.marvisur.sgldqr.api.dto.RespuestaValidacionEntregaDto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Encapsula el resultado de la operación transaccional de validación cruzada y entrega física.
 * Permite que el servicio devuelva el estado de negocio sin lanzar excepciones para fallos de validación,
 * de modo que el controlador pueda traducir cada caso a su código HTTP correspondiente (200, 404, 409).
 */
public class ResultadoProcesoValidacion {

    public enum Tipo {
        EXITOSO,
        FALLIDO_PAGO_PENDIENTE,
        FALLIDO_TOKEN_INVALIDO,
        TRANSICION_INVALIDA,
        NO_ENCONTRADO
    }

    private final Tipo tipo;
    private final RespuestaValidacionEntregaDto respuestaDto;
    private final BigDecimal montoTotalPendiente;
    private final String estadoActual;
    private final String mensaje;
    private final long latenciaMs;

    private ResultadoProcesoValidacion(Tipo tipo, RespuestaValidacionEntregaDto respuestaDto,
                                       BigDecimal montoTotalPendiente, String estadoActual,
                                       String mensaje, long latenciaMs) {
        this.tipo = Objects.requireNonNull(tipo, "El tipo de resultado no puede ser nulo");
        this.respuestaDto = respuestaDto;
        this.montoTotalPendiente = montoTotalPendiente;
        this.estadoActual = estadoActual;
        this.mensaje = mensaje;
        this.latenciaMs = latenciaMs;
    }

    public static ResultadoProcesoValidacion exitoso(RespuestaValidacionEntregaDto respuestaDto, long latenciaMs) {
        return new ResultadoProcesoValidacion(Tipo.EXITOSO, respuestaDto, null, null, respuestaDto.mensaje(), latenciaMs);
    }

    public static ResultadoProcesoValidacion pagoPendiente(BigDecimal montoTotalPendiente, long latenciaMs) {
        return new ResultadoProcesoValidacion(Tipo.FALLIDO_PAGO_PENDIENTE, null, montoTotalPendiente, null,
                "La entrega está bloqueada porque la encomienda se encuentra en modalidad Contra Entrega con pago pendiente.", latenciaMs);
    }

    public static ResultadoProcesoValidacion tokenInvalido(long latenciaMs) {
        return new ResultadoProcesoValidacion(Tipo.FALLIDO_TOKEN_INVALIDO, null, null, null,
                "El código QR2 digital presentado es inválido, ya fue utilizado o no corresponde a esta encomienda.", latenciaMs);
    }

    public static ResultadoProcesoValidacion transicionInvalida(String estadoActual, String mensaje) {
        return new ResultadoProcesoValidacion(Tipo.TRANSICION_INVALIDA, null, null, estadoActual, mensaje, 0);
    }

    public static ResultadoProcesoValidacion noEncontrado(String mensaje) {
        return new ResultadoProcesoValidacion(Tipo.NO_ENCONTRADO, null, null, null, mensaje, 0);
    }

    public Tipo getTipo() {
        return tipo;
    }

    public RespuestaValidacionEntregaDto getRespuestaDto() {
        return respuestaDto;
    }

    public BigDecimal getMontoTotalPendiente() {
        return montoTotalPendiente;
    }

    public String getEstadoActual() {
        return estadoActual;
    }

    public String getMensaje() {
        return mensaje;
    }

    public long getLatenciaMs() {
        return latenciaMs;
    }
}
