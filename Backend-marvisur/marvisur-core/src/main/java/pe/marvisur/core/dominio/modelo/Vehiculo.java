package pe.marvisur.core.dominio.modelo;

/**
 * Vehículo de transporte de Expreso Marvisur.
 * <p>
 * Asociado a un chofer ({@link Usuario} con rol {@link Rol#TN4_CHOFER})
 * y a las rutas que opera durante su jornada.
 * </p>
 */
public class Vehiculo {

    // -------------------------------------------------------------------------
    // Constante de estado disponible
    // -------------------------------------------------------------------------

    /** Valor de estado que indica disponibilidad operativa del vehículo. */
    private static final String ESTADO_DISPONIBLE = "DISPONIBLE";

    // -------------------------------------------------------------------------
    // Atributos
    // -------------------------------------------------------------------------

    /** Identificador único del vehículo (UUID). */
    private final String id;

    /** Placa del vehículo (formato peruano, ej. "ABC-123"). */
    private String placa;

    /** UUID del chofer asignado a este vehículo. */
    private String choferId;

    /**
     * Estado operativo del vehículo.
     * Valores posibles: "DISPONIBLE", "EN_RUTA", "MANTENIMIENTO", "INACTIVO".
     */
    private String estado;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Crea un nuevo vehículo de la flota.
     *
     * @param id       identificador UUID
     * @param placa    placa del vehículo
     * @param choferId UUID del chofer asignado
     * @param estado   estado operativo inicial
     */
    public Vehiculo(String id, String placa, String choferId, String estado) {
        this.id = id;
        this.placa = placa;
        this.choferId = choferId;
        this.estado = estado;
    }

    // -------------------------------------------------------------------------
    // Métodos de dominio
    // -------------------------------------------------------------------------

    /**
     * Verifica si el vehículo está disponible para operar una ruta.
     *
     * @return {@code true} si el estado es {@code "DISPONIBLE"}
     */
    public boolean verificarDisponibilidad() {
        return ESTADO_DISPONIBLE.equals(this.estado);
    }

    // -------------------------------------------------------------------------
    // Getters y Setters
    // -------------------------------------------------------------------------

    /** @return identificador UUID del vehículo */
    public String getId() {
        return id;
    }

    /** @return placa del vehículo */
    public String getPlaca() {
        return placa;
    }

    /** @param placa nueva placa */
    public void setPlaca(String placa) {
        this.placa = placa;
    }

    /** @return UUID del chofer asignado */
    public String getChoferId() {
        return choferId;
    }

    /** @param choferId UUID del nuevo chofer */
    public void setChoferId(String choferId) {
        this.choferId = choferId;
    }

    /** @return estado operativo del vehículo */
    public String getEstado() {
        return estado;
    }

    /** @param estado nuevo estado operativo */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Vehiculo{id='" + id + "', placa='" + placa
                + "', choferId='" + choferId + "', estado='" + estado + "'}";
    }
}
