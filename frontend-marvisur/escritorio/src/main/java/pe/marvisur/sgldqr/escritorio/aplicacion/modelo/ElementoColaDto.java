package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa un ítem almacenado en la cola local SQLite para sincronización offline.
 */
public class ElementoColaDto {

    private final String id;
    private final String payload;
    private final EstadoElementoCola estado;
    private final int intentos;
    private final String ultimoError;
    private final LocalDateTime creadoEn;
    private final LocalDateTime actualizadoEn;

    public ElementoColaDto(String id, String payload, EstadoElementoCola estado, int intentos,
                           String ultimoError, LocalDateTime creadoEn, LocalDateTime actualizadoEn) {
        this.id = Objects.requireNonNull(id, "El id no puede ser nulo");
        this.payload = Objects.requireNonNull(payload, "El payload no puede ser nulo");
        this.estado = Objects.requireNonNull(estado, "El estado no puede ser nulo");
        this.intentos = intentos;
        this.ultimoError = ultimoError;
        this.creadoEn = Objects.requireNonNull(creadoEn, "creadoEn no puede ser nulo");
        this.actualizadoEn = actualizadoEn;
    }

    public String getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    public EstadoElementoCola getEstado() {
        return estado;
    }

    public int getIntentos() {
        return intentos;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public String id() { return id; }
    public String payload() { return payload; }
    public EstadoElementoCola estado() { return estado; }
    public int intentos() { return intentos; }
    public String ultimoError() { return ultimoError; }
    public LocalDateTime creadoEn() { return creadoEn; }
    public LocalDateTime actualizadoEn() { return actualizadoEn; }

    @Override
    public String toString() {
        return "ElementoColaDto{" +
                "id='" + id + '\'' +
                ", estado=" + estado +
                ", intentos=" + intentos +
                ", ultimoError='" + ultimoError + '\'' +
                ", creadoEn=" + creadoEn +
                '}';
    }
}
