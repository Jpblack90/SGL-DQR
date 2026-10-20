package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'tokens_qr2_estatus'.
 */
@Entity
@Table(name = "tokens_qr2_estatus")
public class TokenQr2EstatusJpaEntity {

    @Id
    @Column(name = "id_token_status", length = 36, nullable = false)
    private String idTokenStatus;

    @Column(name = "id_pareja_qr", length = 36, nullable = false)
    private String idParejaQr;

    @Column(name = "estado_token", length = 20, nullable = false)
    private String estadoToken;

    @Column(name = "fecha_invalidacion")
    private OffsetDateTime fechaInvalidacion;

    @Column(name = "motivo_inactivacion", length = 30)
    private String motivoInactivacion;

    public TokenQr2EstatusJpaEntity() {
    }

    public TokenQr2EstatusJpaEntity(String idTokenStatus, String idParejaQr, String estadoToken,
                                    OffsetDateTime fechaInvalidacion, String motivoInactivacion) {
        this.idTokenStatus = idTokenStatus;
        this.idParejaQr = idParejaQr;
        this.estadoToken = estadoToken;
        this.fechaInvalidacion = fechaInvalidacion;
        this.motivoInactivacion = motivoInactivacion;
    }

    public String getIdTokenStatus() {
        return idTokenStatus;
    }

    public void setIdTokenStatus(String idTokenStatus) {
        this.idTokenStatus = idTokenStatus;
    }

    public String getIdParejaQr() {
        return idParejaQr;
    }

    public void setIdParejaQr(String idParejaQr) {
        this.idParejaQr = idParejaQr;
    }

    public String getEstadoToken() {
        return estadoToken;
    }

    public void setEstadoToken(String estadoToken) {
        this.estadoToken = estadoToken;
    }

    public OffsetDateTime getFechaInvalidacion() {
        return fechaInvalidacion;
    }

    public void setFechaInvalidacion(OffsetDateTime fechaInvalidacion) {
        this.fechaInvalidacion = fechaInvalidacion;
    }

    public String getMotivoInactivacion() {
        return motivoInactivacion;
    }

    public void setMotivoInactivacion(String motivoInactivacion) {
        this.motivoInactivacion = motivoInactivacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenQr2EstatusJpaEntity that = (TokenQr2EstatusJpaEntity) o;
        return Objects.equals(idTokenStatus, that.idTokenStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTokenStatus);
    }
}
