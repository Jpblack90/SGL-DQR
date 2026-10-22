package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'parejas_doble_qr'.
 */
@Entity
@Table(name = "parejas_doble_qr")
public class ParDobleQrJpaEntity {

    @Id
    @Column(name = "id_pareja_qr", length = 36, nullable = false)
    private String idParejaQr;

    @Column(name = "id_encomienda", length = 36, nullable = false, unique = true)
    private String idEncomienda;

    @Column(name = "uuid_qr1_operativo", length = 100, nullable = false)
    private String uuidQr1Operativo;

    @Column(name = "hash_qr2_llave_digital", length = 255, nullable = false)
    private String hashQr2LlaveDigital;

    @Column(name = "fecha_generacion", nullable = false)
    private OffsetDateTime fechaGeneracion;

    public ParDobleQrJpaEntity() {
    }

    public ParDobleQrJpaEntity(String idParejaQr, String idEncomienda, String uuidQr1Operativo,
                               String hashQr2LlaveDigital, OffsetDateTime fechaGeneracion) {
        this.idParejaQr = idParejaQr;
        this.idEncomienda = idEncomienda;
        this.uuidQr1Operativo = uuidQr1Operativo;
        this.hashQr2LlaveDigital = hashQr2LlaveDigital;
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getIdParejaQr() {
        return idParejaQr;
    }

    public void setIdParejaQr(String idParejaQr) {
        this.idParejaQr = idParejaQr;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public void setIdEncomienda(String idEncomienda) {
        this.idEncomienda = idEncomienda;
    }

    public String getUuidQr1Operativo() {
        return uuidQr1Operativo;
    }

    public void setUuidQr1Operativo(String uuidQr1Operativo) {
        this.uuidQr1Operativo = uuidQr1Operativo;
    }

    public String getHashQr2LlaveDigital() {
        return hashQr2LlaveDigital;
    }

    public void setHashQr2LlaveDigital(String hashQr2LlaveDigital) {
        this.hashQr2LlaveDigital = hashQr2LlaveDigital;
    }

    public OffsetDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(OffsetDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParDobleQrJpaEntity that = (ParDobleQrJpaEntity) o;
        return Objects.equals(idParejaQr, that.idParejaQr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idParejaQr);
    }
}
