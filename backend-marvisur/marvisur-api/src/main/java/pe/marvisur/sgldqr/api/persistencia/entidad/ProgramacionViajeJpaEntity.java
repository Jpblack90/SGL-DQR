package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'programacion_viajes'.
 */
@Entity
@Table(name = "programacion_viajes")
public class ProgramacionViajeJpaEntity {

    @Id
    @Column(name = "id_viaje", length = 36, nullable = false)
    private String idViaje;

    @Column(name = "id_ruta", length = 36, nullable = false)
    private String idRuta;

    @Column(name = "id_vehiculo", length = 36, nullable = false)
    private String idVehiculo;

    @Column(name = "id_chofer", length = 36, nullable = false)
    private String idChofer;

    @Column(name = "fecha_salida", nullable = false)
    private OffsetDateTime fechaSalida;

    @Column(name = "fecha_llegada_estimada", nullable = false)
    private OffsetDateTime fechaLlegadaEstimada;

    @Column(name = "estado_viaje", length = 30, nullable = false)
    private String estadoViaje;

    public ProgramacionViajeJpaEntity() {
    }

    public ProgramacionViajeJpaEntity(String idViaje, String idRuta, String idVehiculo, String idChofer,
                                      OffsetDateTime fechaSalida, OffsetDateTime fechaLlegadaEstimada, String estadoViaje) {
        this.idViaje = idViaje;
        this.idRuta = idRuta;
        this.idVehiculo = idVehiculo;
        this.idChofer = idChofer;
        this.fechaSalida = fechaSalida;
        this.fechaLlegadaEstimada = fechaLlegadaEstimada;
        this.estadoViaje = estadoViaje;
    }

    public String getIdViaje() {
        return idViaje;
    }

    public void setIdViaje(String idViaje) {
        this.idViaje = idViaje;
    }

    public String getIdRuta() {
        return idRuta;
    }

    public void setIdRuta(String idRuta) {
        this.idRuta = idRuta;
    }

    public String getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(String idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public String getIdChofer() {
        return idChofer;
    }

    public void setIdChofer(String idChofer) {
        this.idChofer = idChofer;
    }

    public OffsetDateTime getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(OffsetDateTime fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public OffsetDateTime getFechaLlegadaEstimada() {
        return fechaLlegadaEstimada;
    }

    public void setFechaLlegadaEstimada(OffsetDateTime fechaLlegadaEstimada) {
        this.fechaLlegadaEstimada = fechaLlegadaEstimada;
    }

    public String getEstadoViaje() {
        return estadoViaje;
    }

    public void setEstadoViaje(String estadoViaje) {
        this.estadoViaje = estadoViaje;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgramacionViajeJpaEntity that = (ProgramacionViajeJpaEntity) o;
        return Objects.equals(idViaje, that.idViaje);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idViaje);
    }
}
