package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'encomiendas'.
 */
@Entity
@Table(name = "encomiendas")
public class EncomiendaJpaEntity {

    @Id
    @Column(name = "id_encomienda", length = 36, nullable = false)
    private String idEncomienda;

    @Column(name = "codigo_comprobante", length = 50, nullable = false, unique = true)
    private String codigoComprobante;

    @Column(name = "id_remitente", length = 36, nullable = false)
    private String idRemitente;

    @Column(name = "id_destinatario", length = 36, nullable = false)
    private String idDestinatario;

    @Column(name = "id_sucursal_origen", length = 36, nullable = false)
    private String idSucursalOrigen;

    @Column(name = "id_sucursal_destino", length = 36, nullable = false)
    private String idSucursalDestino;

    @Column(name = "peso_kg", precision = 8, scale = 2, nullable = false)
    private BigDecimal pesoKg;

    @Column(name = "volumen_m3", precision = 8, scale = 4)
    private BigDecimal volumenM3;

    @Column(name = "descripcion_contenido", columnDefinition = "TEXT")
    private String descripcionContenido;

    @Column(name = "monto_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoTotal;

    @Column(name = "estado_pago", length = 20, nullable = false)
    private String estadoPago;

    @Column(name = "estado_logistico", length = 25, nullable = false)
    private String estadoLogistico;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    public EncomiendaJpaEntity() {
    }

    public EncomiendaJpaEntity(String idEncomienda, String codigoComprobante, String idRemitente,
                               String idDestinatario, String idSucursalOrigen, String idSucursalDestino,
                               BigDecimal pesoKg, BigDecimal volumenM3, String descripcionContenido,
                               BigDecimal montoTotal, String estadoPago, String estadoLogistico,
                               OffsetDateTime fechaRegistro) {
        this.idEncomienda = idEncomienda;
        this.codigoComprobante = codigoComprobante;
        this.idRemitente = idRemitente;
        this.idDestinatario = idDestinatario;
        this.idSucursalOrigen = idSucursalOrigen;
        this.idSucursalDestino = idSucursalDestino;
        this.pesoKg = pesoKg;
        this.volumenM3 = volumenM3;
        this.descripcionContenido = descripcionContenido;
        this.montoTotal = montoTotal;
        this.estadoPago = estadoPago;
        this.estadoLogistico = estadoLogistico;
        this.fechaRegistro = fechaRegistro;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public void setIdEncomienda(String idEncomienda) {
        this.idEncomienda = idEncomienda;
    }

    public String getCodigoComprobante() {
        return codigoComprobante;
    }

    public void setCodigoComprobante(String codigoComprobante) {
        this.codigoComprobante = codigoComprobante;
    }

    public String getIdRemitente() {
        return idRemitente;
    }

    public void setIdRemitente(String idRemitente) {
        this.idRemitente = idRemitente;
    }

    public String getIdDestinatario() {
        return idDestinatario;
    }

    public void setIdDestinatario(String idDestinatario) {
        this.idDestinatario = idDestinatario;
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

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public BigDecimal getVolumenM3() {
        return volumenM3;
    }

    public void setVolumenM3(BigDecimal volumenM3) {
        this.volumenM3 = volumenM3;
    }

    public String getDescripcionContenido() {
        return descripcionContenido;
    }

    public void setDescripcionContenido(String descripcionContenido) {
        this.descripcionContenido = descripcionContenido;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public String getEstadoLogistico() {
        return estadoLogistico;
    }

    public void setEstadoLogistico(String estadoLogistico) {
        this.estadoLogistico = estadoLogistico;
    }

    public OffsetDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(OffsetDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncomiendaJpaEntity that = (EncomiendaJpaEntity) o;
        return Objects.equals(idEncomienda, that.idEncomienda);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEncomienda);
    }
}
