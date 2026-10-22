package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'vehiculos'.
 */
@Entity
@Table(name = "vehiculos")
public class VehiculoJpaEntity {

    @Id
    @Column(name = "id_vehiculo", length = 36, nullable = false)
    private String idVehiculo;

    @Column(name = "placa", length = 10, nullable = false, unique = true)
    private String placa;

    @Column(name = "modelo", length = 100, nullable = false)
    private String modelo;

    @Column(name = "capacidad_carga_kg", precision = 10, scale = 2, nullable = false)
    private BigDecimal capacidadCargaKg;

    public VehiculoJpaEntity() {
    }

    public VehiculoJpaEntity(String idVehiculo, String placa, String modelo, BigDecimal capacidadCargaKg) {
        this.idVehiculo = idVehiculo;
        this.placa = placa;
        this.modelo = modelo;
        this.capacidadCargaKg = capacidadCargaKg;
    }

    public String getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(String idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public BigDecimal getCapacidadCargaKg() {
        return capacidadCargaKg;
    }

    public void setCapacidadCargaKg(BigDecimal capacidadCargaKg) {
        this.capacidadCargaKg = capacidadCargaKg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehiculoJpaEntity that = (VehiculoJpaEntity) o;
        return Objects.equals(idVehiculo, that.idVehiculo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVehiculo);
    }
}
