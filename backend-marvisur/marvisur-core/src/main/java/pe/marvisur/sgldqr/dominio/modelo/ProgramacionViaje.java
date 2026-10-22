package pe.marvisur.sgldqr.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una programación de viaje interprovincial
 * según la tabla programacion_viajes del Diccionario de Datos.
 */
public class ProgramacionViaje {

    private final String idViaje;
    private final String idRuta;
    private final String idVehiculo;
    private final String idChofer;
    private final LocalDateTime fechaSalida;
    private final LocalDateTime fechaLlegadaEstimada;
    private EstadoViaje estadoViaje;

    public ProgramacionViaje(String idViaje, String idRuta, String idVehiculo, String idChofer,
                             LocalDateTime fechaSalida, LocalDateTime fechaLlegadaEstimada, EstadoViaje estadoViaje) {
        this.idViaje = Objects.requireNonNull(idViaje, "El idViaje no puede ser nulo");
        this.idRuta = Objects.requireNonNull(idRuta, "El idRuta no puede ser nulo");
        this.idVehiculo = Objects.requireNonNull(idVehiculo, "El idVehiculo no puede ser nulo");
        this.idChofer = Objects.requireNonNull(idChofer, "El idChofer no puede ser nulo");
        this.fechaSalida = Objects.requireNonNull(fechaSalida, "La fechaSalida no puede ser nula");
        this.fechaLlegadaEstimada = Objects.requireNonNull(fechaLlegadaEstimada, "La fechaLlegadaEstimada no puede ser nula");
        this.estadoViaje = Objects.requireNonNull(estadoViaje, "El estadoViaje no puede ser nulo");
    }

    public static ProgramacionViaje crear(String idRuta, String idVehiculo, String idChofer,
                                          LocalDateTime fechaSalida, LocalDateTime fechaLlegadaEstimada) {
        return new ProgramacionViaje(
                UUID.randomUUID().toString(),
                idRuta,
                idVehiculo,
                idChofer,
                fechaSalida,
                fechaLlegadaEstimada,
                EstadoViaje.PROGRAMADO
        );
    }

    public void actualizarEstado(EstadoViaje nuevoEstado) {
        this.estadoViaje = Objects.requireNonNull(nuevoEstado, "El nuevo estado del viaje no puede ser nulo");
    }

    public String getIdViaje() {
        return idViaje;
    }

    public String getIdRuta() {
        return idRuta;
    }

    public String getIdVehiculo() {
        return idVehiculo;
    }

    public String getIdChofer() {
        return idChofer;
    }

    public LocalDateTime getFechaSalida() {
        return fechaSalida;
    }

    public LocalDateTime getFechaLlegadaEstimada() {
        return fechaLlegadaEstimada;
    }

    public EstadoViaje getEstadoViaje() {
        return estadoViaje;
    }
}
