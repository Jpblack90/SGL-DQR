package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.AlmacenPort;

import java.math.BigDecimal;

/**
 * ViewModel que gestiona el estado y ciclo de vida de la validación cruzada y entrega física.
 * Mantiene los códigos QR en memoria durante el bloqueo por pago Contra Entrega, y los
 * destruye/limpia inmediatamente tras la entrega exitosa o rechazo definitivo.
 */
public class EntregaFlujoViewModel {

    public enum EstadoFlujo {
        INICIAL,
        EXITOSO,
        BLOQUEADO_PAGO_PENDIENTE,
        PAGO_REGISTRADO,
        RECHAZADO_TOKEN_INVALIDO,
        RECHAZADO_TRANSICION_INVALIDA,
        RECHAZADO_NO_ENCONTRADO,
        ERROR
    }

    private final AlmacenPort almacenPort;

    private String qr1EnMemoria;
    private String qr2EnMemoria;
    private String idEncomiendaActual;
    private String codigoComprobanteActual;
    private BigDecimal montoPendienteActual;
    private EstadoFlujo estadoActual = EstadoFlujo.INICIAL;
    private String ultimoMensaje;
    private long ultimaLatenciaMs;

    public EntregaFlujoViewModel(AlmacenPort almacenPort) {
        this.almacenPort = almacenPort;
    }

    public void setQr1(String qr1) {
        this.qr1EnMemoria = qr1 != null ? qr1.trim() : null;
    }

    public void setQr2(String qr2) {
        this.qr2EnMemoria = qr2 != null ? qr2.trim() : null;
    }

    public ValidacionEntregaResultado procesarValidacion() {
        if (qr1EnMemoria == null || qr1EnMemoria.isBlank() || qr2EnMemoria == null || qr2EnMemoria.isBlank()) {
            this.estadoActual = EstadoFlujo.ERROR;
            this.ultimoMensaje = "Debe escanear tanto el QR1 como el QR2.";
            return new ValidacionEntregaResultado(
                    ValidacionEntregaResultado.TipoValidacion.ERROR_INESPERADO,
                    null, null, null, null, 0, this.ultimoMensaje
            );
        }

        ValidacionEntregaResultado resultado = almacenPort.validarEntrega(qr1EnMemoria, qr2EnMemoria);
        this.ultimaLatenciaMs = resultado.latenciaMs();
        this.ultimoMensaje = resultado.mensaje();

        switch (resultado.tipo()) {
            case EXITOSO -> {
                this.estadoActual = EstadoFlujo.EXITOSO;
                this.idEncomiendaActual = resultado.idEncomienda();
                this.codigoComprobanteActual = resultado.codigoComprobante();
                limpiarSecretos();
            }
            case PAGO_PENDIENTE -> {
                this.estadoActual = EstadoFlujo.BLOQUEADO_PAGO_PENDIENTE;
                this.idEncomiendaActual = resultado.idEncomienda();
                this.codigoComprobanteActual = resultado.codigoComprobante();
                this.montoPendienteActual = resultado.montoPendiente();
                // NOTA CRÍTICA: Se conservan qr1EnMemoria y qr2EnMemoria para permitir reintento inmediato tras cobro
            }
            case TOKEN_INVALIDO -> {
                this.estadoActual = EstadoFlujo.RECHAZADO_TOKEN_INVALIDO;
                limpiarSecretos();
            }
            case TRANSICION_INVALIDA -> {
                this.estadoActual = EstadoFlujo.RECHAZADO_TRANSICION_INVALIDA;
                limpiarSecretos();
            }
            case NO_ENCONTRADO -> {
                this.estadoActual = EstadoFlujo.RECHAZADO_NO_ENCONTRADO;
                limpiarSecretos();
            }
            case ERROR_INESPERADO -> {
                this.estadoActual = EstadoFlujo.ERROR;
                limpiarSecretos();
            }
        }

        return resultado;
    }

    public RespuestaCobroDto procesarCobro() {
        if (idEncomiendaActual == null || idEncomiendaActual.isBlank()) {
            throw new IllegalStateException("No hay una encomienda bloqueada para registrar cobro.");
        }

        RespuestaCobroDto respuesta = almacenPort.registrarCobro(idEncomiendaActual);
        this.estadoActual = EstadoFlujo.PAGO_REGISTRADO;
        this.ultimoMensaje = respuesta.mensaje();
        this.montoPendienteActual = BigDecimal.ZERO;
        return respuesta;
    }

    public ValidacionEntregaResultado reintentarValidacion() {
        if (qr1EnMemoria == null || qr2EnMemoria == null) {
            throw new IllegalStateException("No hay códigos QR en memoria para reintentar la validación.");
        }
        return procesarValidacion();
    }

    public void limpiarTodo() {
        limpiarSecretos();
        this.idEncomiendaActual = null;
        this.codigoComprobanteActual = null;
        this.montoPendienteActual = null;
        this.estadoActual = EstadoFlujo.INICIAL;
        this.ultimoMensaje = null;
        this.ultimaLatenciaMs = 0;
    }

    public void limpiarSecretos() {
        this.qr1EnMemoria = null;
        this.qr2EnMemoria = null;
    }

    public String getQr1EnMemoria() {
        return qr1EnMemoria;
    }

    public String getQr2EnMemoria() {
        return qr2EnMemoria;
    }

    public EstadoFlujo getEstadoActual() {
        return estadoActual;
    }

    public String getIdEncomiendaActual() {
        return idEncomiendaActual;
    }

    public String getCodigoComprobanteActual() {
        return codigoComprobanteActual;
    }

    public BigDecimal getMontoPendienteActual() {
        return montoPendienteActual;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public long getUltimaLatenciaMs() {
        return ultimaLatenciaMs;
    }
}
