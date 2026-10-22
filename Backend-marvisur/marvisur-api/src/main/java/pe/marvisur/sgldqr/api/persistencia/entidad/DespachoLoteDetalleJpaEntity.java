package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'despachos_lote_detalles'.
 */
@Entity
@Table(name = "despachos_lote_detalles")
public class DespachoLoteDetalleJpaEntity {

    @Id
    @Column(name = "id_despacho_detalle", length = 36, nullable = false)
    private String idDespachoDetalle;

    @Column(name = "id_viaje", length = 36, nullable = false)
    private String idViaje;

    @Column(name = "id_encomienda", length = 36, nullable = false)
    private String idEncomienda;

    @Column(name = "fecha_asociacion", nullable = false)
    private OffsetDateTime fechaAsociacion;

    public DespachoLoteDetalleJpaEntity() {
    }

    public DespachoLoteDetalleJpaEntity(String idDespachoDetalle, String idViaje, String idEncomienda, OffsetDateTime fechaAsociacion) {
        this.idDespachoDetalle = idDespachoDetalle;
        this.idViaje = idViaje;
        this.idEncomienda = idEncomienda;
        this.fechaAsociacion = fechaAsociacion;
    }

    public String getIdDespachoDetalle() {
        return idDespachoDetalle;
    }

    public void setIdDespachoDetalle(String idDespachoDetalle) {
        this.idDespachoDetalle = idDespachoDetalle;
    }

    public String getIdViaje() {
        return idViaje;
    }

    public void setIdViaje(String idViaje) {
        this.idViaje = idViaje;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public void setIdEncomienda(String idEncomienda) {
        this.idEncomienda = idEncomienda;
    }

    public OffsetDateTime getFechaAsociacion() {
        return fechaAsociacion;
    }

    public void setFechaAsociacion(OffsetDateTime fechaAsociacion) {
        this.fechaAsociacion = fechaAsociacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DespachoLoteDetalleJpaEntity that = (DespachoLoteDetalleJpaEntity) o;
        return Objects.equals(idDespachoDetalle, that.idDespachoDetalle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDespachoDetalle);
    }
}
