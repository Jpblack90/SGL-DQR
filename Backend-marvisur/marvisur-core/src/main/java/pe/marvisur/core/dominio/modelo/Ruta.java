package pe.marvisur.core.dominio.modelo;

import java.time.LocalTime;

/**
 * Ruta de transporte entre dos agencias de Expreso Marvisur.
 * <p>
 * Una ruta define el trayecto entre {@code agenciaOrigenId} y {@code agenciaDestinoId},
 * el horario de salida y el estado operativo del viaje.
 * </p>
 */
public class Ruta {

    // -------------------------------------------------------------------------
    // Atributos
    // -------------------------------------------------------------------------

    /** Identificador único de la ruta (UUID). */
    private final String id;

    /** UUID de la agencia de origen. */
    private String agenciaOrigenId;

    /** UUID de la agencia de destino. */
    private String agenciaDestinoId;

    /**
     * Estado operativo de la ruta.
     * Valores posibles: "PROGRAMADA", "EN_RUTA", "LLEGADA_REGISTRADA", "CANCELADA".
     */
    private String estado;

    /** Hora programada de salida desde la agencia de origen. */
    private LocalTime horaSalida;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Crea una nueva ruta de transporte.
     *
     * @param id                identificador UUID
     * @param agenciaOrigenId   UUID de la agencia de origen
     * @param agenciaDestinoId  UUID de la agencia de destino
     * @param estado            estado operativo inicial
     * @param horaSalida        hora programada de salida
     */
    public Ruta(String id, String agenciaOrigenId, String agenciaDestinoId,
                String estado, LocalTime horaSalida) {
        this.id = id;
        this.agenciaOrigenId = agenciaOrigenId;
        this.agenciaDestinoId = agenciaDestinoId;
        this.estado = estado;
        this.horaSalida = horaSalida;
    }

    // -------------------------------------------------------------------------
    // Métodos de dominio
    // -------------------------------------------------------------------------

    /**
     * Registra la llegada del vehículo a la agencia de destino.
     * <p>
     * Cambia el estado de la ruta a {@code "LLEGADA_REGISTRADA"}.
     * </p>
     *
     * @return {@code true} si la llegada fue registrada correctamente
     */
    public boolean registrarLlegada() {
        this.estado = "LLEGADA_REGISTRADA";
        return true;
    }

    // -------------------------------------------------------------------------
    // Getters y Setters
    // -------------------------------------------------------------------------

    /** @return identificador UUID de la ruta */
    public String getId() {
        return id;
    }

    /** @return UUID de la agencia de origen */
    public String getAgenciaOrigenId() {
        return agenciaOrigenId;
    }

    /** @param agenciaOrigenId nuevo UUID de origen */
    public void setAgenciaOrigenId(String agenciaOrigenId) {
        this.agenciaOrigenId = agenciaOrigenId;
    }

    /** @return UUID de la agencia de destino */
    public String getAgenciaDestinoId() {
        return agenciaDestinoId;
    }

    /** @param agenciaDestinoId nuevo UUID de destino */
    public void setAgenciaDestinoId(String agenciaDestinoId) {
        this.agenciaDestinoId = agenciaDestinoId;
    }

    /** @return estado operativo de la ruta */
    public String getEstado() {
        return estado;
    }

    /** @param estado nuevo estado operativo */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    /** @return hora programada de salida */
    public LocalTime getHoraSalida() {
        return horaSalida;
    }

    /** @param horaSalida nueva hora de salida */
    public void setHoraSalida(LocalTime horaSalida) {
        this.horaSalida = horaSalida;
    }

    @Override
    public String toString() {
        return "Ruta{id='" + id + "', origen='" + agenciaOrigenId
                + "', destino='" + agenciaDestinoId + "', estado='" + estado
                + "', salida=" + horaSalida + '}';
    }
}
