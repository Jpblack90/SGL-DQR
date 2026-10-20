package pe.marvisur.sgldqr.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Objeto de valor que encapsula la respuesta completa de una validación cruzada Doble QR,
 * incluyendo el resultado oficial y la métrica de latencia para auditoría (RNF-03).
 */
public class ResultadoValidacionCruzada {

    private final ResultadoValidacion resultado;
    private final long latenciaProcesamientoMs;
    private final String mensaje;
    private final LocalDateTime fechaValidacion;

    public ResultadoValidacionCruzada(ResultadoValidacion resultado, long latenciaProcesamientoMs, String mensaje, LocalDateTime fechaValidacion) {
        this.resultado = Objects.requireNonNull(resultado, "El resultado no puede ser nulo");
        this.latenciaProcesamientoMs = latenciaProcesamientoMs;
        this.mensaje = Objects.requireNonNull(mensaje, "El mensaje no puede ser nulo");
        this.fechaValidacion = Objects.requireNonNull(fechaValidacion, "La fechaValidacion no puede ser nula");
    }

    public static ResultadoValidacionCruzada exitoso(long latenciaMs, LocalDateTime fecha) {
        return new ResultadoValidacionCruzada(ResultadoValidacion.EXITOSO, latenciaMs, "Validación cruzada exitosa. El token QR2 ha sido invalidado.", fecha);
    }

    public static ResultadoValidacionCruzada pagoPendiente(long latenciaMs, LocalDateTime fecha) {
        return new ResultadoValidacionCruzada(ResultadoValidacion.FALLIDO_PAGO_PENDIENTE, latenciaMs, "La encomienda tiene pago PENDIENTE (Contra Entrega). Entrega bloqueada.", fecha);
    }

    public static ResultadoValidacionCruzada tokenInvalido(long latenciaMs, String detalle, LocalDateTime fecha) {
        return new ResultadoValidacionCruzada(ResultadoValidacion.FALLIDO_TOKEN_INVALIDO, latenciaMs, detalle, fecha);
    }

    public boolean esExitoso() {
        return this.resultado == ResultadoValidacion.EXITOSO;
    }

    public ResultadoValidacion getResultado() {
        return resultado;
    }

    public long getLatenciaProcesamientoMs() {
        return latenciaProcesamientoMs;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    @Override
    public String toString() {
        return "ResultadoValidacionCruzada{" +
                "resultado=" + resultado +
                ", latenciaProcesamientoMs=" + latenciaProcesamientoMs +
                ", mensaje='" + mensaje + '\'' +
                ", fechaValidacion=" + fechaValidacion +
                '}';
    }
}
