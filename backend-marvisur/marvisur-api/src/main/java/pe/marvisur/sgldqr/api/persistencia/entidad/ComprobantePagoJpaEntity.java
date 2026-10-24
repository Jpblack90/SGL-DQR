package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'comprobantes_pago' (V6__modulo6_tracking_comprobantes.sql).
 */
@Entity
@Table(name = "comprobantes_pago")
public class ComprobantePagoJpaEntity {

    @Id
    @Column(name = "id_comprobante", length = 36, nullable = false)
    private String idComprobante;

    @Column(name = "id_encomienda", length = 36, nullable = false, unique = true)
    private String idEncomienda;

    @Column(name = "tipo_comprobante", length = 20, nullable = false)
    private String tipoComprobante;

    @Column(name = "serie_correlativo", length = 50, nullable = false)
    private String serieCorrelativo;

    @Column(name = "monto_subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoSubtotal;

    @Column(name = "monto_igv", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoIgv;

    @Column(name = "monto_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoTotal;

    @Column(name = "fecha_emision", nullable = false)
    private OffsetDateTime fechaEmision;

    public ComprobantePagoJpaEntity() {
    }

    public ComprobantePagoJpaEntity(String idComprobante, String idEncomienda, String tipoComprobante,
                                    String serieCorrelativo, BigDecimal montoSubtotal, BigDecimal montoIgv,
                                    BigDecimal montoTotal, OffsetDateTime fechaEmision) {
        this.idComprobante = idComprobante;
        this.idEncomienda = idEncomienda;
        this.tipoComprobante = tipoComprobante;
        this.serieCorrelativo = serieCorrelativo;
        this.montoSubtotal = montoSubtotal;
        this.montoIgv = montoIgv;
        this.montoTotal = montoTotal;
        this.fechaEmision = fechaEmision;
    }

    public String getIdComprobante() {
        return idComprobante;
    }

    public void setIdComprobante(String idComprobante) {
        this.idComprobante = idComprobante;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public void setIdEncomienda(String idEncomienda) {
        this.idEncomienda = idEncomienda;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getSerieCorrelativo() {
        return serieCorrelativo;
    }

    public void setSerieCorrelativo(String serieCorrelativo) {
        this.serieCorrelativo = serieCorrelativo;
    }

    public BigDecimal getMontoSubtotal() {
        return montoSubtotal;
    }

    public void setMontoSubtotal(BigDecimal montoSubtotal) {
        this.montoSubtotal = montoSubtotal;
    }

    public BigDecimal getMontoIgv() {
        return montoIgv;
    }

    public void setMontoIgv(BigDecimal montoIgv) {
        this.montoIgv = montoIgv;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public OffsetDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(OffsetDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComprobantePagoJpaEntity that = (ComprobantePagoJpaEntity) o;
        return Objects.equals(idComprobante, that.idComprobante);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idComprobante);
    }
}
