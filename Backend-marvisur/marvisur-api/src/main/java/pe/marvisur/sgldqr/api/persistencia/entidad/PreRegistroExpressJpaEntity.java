package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'pre_registros_express' (Módulo 3: Clientes y Pre-Registros, RF-10).
 */
@Entity
@Table(name = "pre_registros_express")
public class PreRegistroExpressJpaEntity {

    @Id
    @Column(name = "id_preregistro", length = 36, nullable = false)
    private String idPreRegistro;

    @Column(name = "codigo_express", length = 50, nullable = false, unique = true)
    private String codigoExpress;

    @Column(name = "id_cliente_remitente", length = 36, nullable = false)
    private String idClienteRemitente;

    @Column(name = "id_cliente_destinatario", length = 36, nullable = false)
    private String idClienteDestinatario;

    @Column(name = "id_sucursal_destino", length = 36, nullable = false)
    private String idSucursalDestino;

    @Column(name = "peso_estimado", precision = 8, scale = 3, nullable = false)
    private BigDecimal pesoEstimado;

    @Column(name = "monto_cotizado", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoCotizado;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "estado", length = 30, nullable = false)
    private String estado;

    public PreRegistroExpressJpaEntity() {
    }

    public PreRegistroExpressJpaEntity(String idPreRegistro, String codigoExpress,
                                       String idClienteRemitente, String idClienteDestinatario,
                                       String idSucursalDestino, BigDecimal pesoEstimado,
                                       BigDecimal montoCotizado, OffsetDateTime fechaCreacion,
                                       String estado) {
        this.idPreRegistro = idPreRegistro;
        this.codigoExpress = codigoExpress;
        this.idClienteRemitente = idClienteRemitente;
        this.idClienteDestinatario = idClienteDestinatario;
        this.idSucursalDestino = idSucursalDestino;
        this.pesoEstimado = pesoEstimado;
        this.montoCotizado = montoCotizado;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
    }

    public String getIdPreRegistro() {
        return idPreRegistro;
    }

    public void setIdPreRegistro(String idPreRegistro) {
        this.idPreRegistro = idPreRegistro;
    }

    public String getCodigoExpress() {
        return codigoExpress;
    }

    public void setCodigoExpress(String codigoExpress) {
        this.codigoExpress = codigoExpress;
    }

    public String getIdClienteRemitente() {
        return idClienteRemitente;
    }

    public void setIdClienteRemitente(String idClienteRemitente) {
        this.idClienteRemitente = idClienteRemitente;
    }

    public String getIdClienteDestinatario() {
        return idClienteDestinatario;
    }

    public void setIdClienteDestinatario(String idClienteDestinatario) {
        this.idClienteDestinatario = idClienteDestinatario;
    }

    public String getIdSucursalDestino() {
        return idSucursalDestino;
    }

    public void setIdSucursalDestino(String idSucursalDestino) {
        this.idSucursalDestino = idSucursalDestino;
    }

    public BigDecimal getPesoEstimado() {
        return pesoEstimado;
    }

    public void setPesoEstimado(BigDecimal pesoEstimado) {
        this.pesoEstimado = pesoEstimado;
    }

    public BigDecimal getMontoCotizado() {
        return montoCotizado;
    }

    public void setMontoCotizado(BigDecimal montoCotizado) {
        this.montoCotizado = montoCotizado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreRegistroExpressJpaEntity that = (PreRegistroExpressJpaEntity) o;
        return Objects.equals(idPreRegistro, that.idPreRegistro);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPreRegistro);
    }
}
