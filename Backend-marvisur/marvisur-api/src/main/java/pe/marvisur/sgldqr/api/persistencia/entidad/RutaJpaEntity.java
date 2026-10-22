package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'rutas'.
 */
@Entity
@Table(name = "rutas")
public class RutaJpaEntity {

    @Id
    @Column(name = "id_ruta", length = 36, nullable = false)
    private String idRuta;

    @Column(name = "id_sucursal_origen", length = 36, nullable = false)
    private String idSucursalOrigen;

    @Column(name = "id_sucursal_destino", length = 36, nullable = false)
    private String idSucursalDestino;

    @Column(name = "distancia_km", precision = 10, scale = 2, nullable = false)
    private BigDecimal distanciaKm;

    public RutaJpaEntity() {
    }

    public RutaJpaEntity(String idRuta, String idSucursalOrigen, String idSucursalDestino, BigDecimal distanciaKm) {
        this.idRuta = idRuta;
        this.idSucursalOrigen = idSucursalOrigen;
        this.idSucursalDestino = idSucursalDestino;
        this.distanciaKm = distanciaKm;
    }

    public String getIdRuta() {
        return idRuta;
    }

    public void setIdRuta(String idRuta) {
        this.idRuta = idRuta;
    }

    public String getIdSucursalOrigen() {
        return idSucursalOrigen;
    }

    public void setIdSucursalOrigen(String idSucursalOrigen) {
        this.idSucursalOrigen = idSucursalOrigen;
    }

    public String getIdSucursalDestino() {
        return idSucursalDestino;
    }

    public void setIdSucursalDestino(String idSucursalDestino) {
        this.idSucursalDestino = idSucursalDestino;
    }

    public BigDecimal getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(BigDecimal distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RutaJpaEntity that = (RutaJpaEntity) o;
        return Objects.equals(idRuta, that.idRuta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRuta);
    }
}
