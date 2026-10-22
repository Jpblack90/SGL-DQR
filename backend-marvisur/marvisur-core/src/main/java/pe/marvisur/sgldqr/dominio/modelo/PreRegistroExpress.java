package pe.marvisur.sgldqr.dominio.modelo;

import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionDominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa un Pre-Registro Web temporal ('Código Express')
 * generado por el cliente final para agilizar la atención en ventanilla (RF-10).
 */
public class PreRegistroExpress {

    private final String idPreRegistro;
    private final String codigoExpress;
    private final String idClienteRemitente;
    private final String idClienteDestinatario;
    private final String idSucursalDestino;
    private final BigDecimal pesoEstimado;
    private final BigDecimal montoCotizado;
    private final LocalDateTime fechaCreacion;
    private EstadoPreRegistro estado;

    public PreRegistroExpress(String idPreRegistro, String codigoExpress,
                              String idClienteRemitente, String idClienteDestinatario,
                              String idSucursalDestino, BigDecimal pesoEstimado,
                              BigDecimal montoCotizado, LocalDateTime fechaCreacion,
                              EstadoPreRegistro estado) {
        this.idPreRegistro = Objects.requireNonNull(idPreRegistro, "El idPreRegistro no puede ser nulo");
        this.codigoExpress = Objects.requireNonNull(codigoExpress, "El codigoExpress no puede ser nulo");
        this.idClienteRemitente = Objects.requireNonNull(idClienteRemitente, "El idClienteRemitente no puede ser nulo");
        this.idClienteDestinatario = Objects.requireNonNull(idClienteDestinatario, "El idClienteDestinatario no puede ser nulo");
        this.idSucursalDestino = Objects.requireNonNull(idSucursalDestino, "El idSucursalDestino no puede ser nulo");
        this.pesoEstimado = Objects.requireNonNull(pesoEstimado, "El pesoEstimado no puede ser nulo");
        this.montoCotizado = Objects.requireNonNull(montoCotizado, "El montoCotizado no puede ser nulo");
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "La fechaCreacion no puede ser nula");
        this.estado = Objects.requireNonNull(estado, "El estado no puede ser nulo");
    }

    public static PreRegistroExpress crearNuevo(String codigoExpress,
                                               String idClienteRemitente,
                                               String idClienteDestinatario,
                                               String idSucursalDestino,
                                               BigDecimal pesoEstimado,
                                               BigDecimal montoCotizado,
                                               LocalDateTime fechaCreacion) {
        return new PreRegistroExpress(
                UUID.randomUUID().toString(),
                codigoExpress,
                idClienteRemitente,
                idClienteDestinatario,
                idSucursalDestino,
                pesoEstimado,
                montoCotizado,
                fechaCreacion,
                EstadoPreRegistro.PENDIENTE_VENTANILLA
        );
    }

    public boolean estaExpirado(LocalDateTime ahora, int vigenciaHoras) {
        if (this.estado == EstadoPreRegistro.EXPIRADO) {
            return true;
        }
        return ahora.isAfter(this.fechaCreacion.plusHours(vigenciaHoras));
    }

    public LocalDateTime calcularFechaExpiracion(int vigenciaHoras) {
        return this.fechaCreacion.plusHours(vigenciaHoras);
    }

    public void marcarProcesado(LocalDateTime ahora, int vigenciaHoras) {
        if (estaExpirado(ahora, vigenciaHoras)) {
            this.estado = EstadoPreRegistro.EXPIRADO;
            throw new ExcepcionDominio("El pre-registro express con código " + codigoExpress + " ha expirado y no puede procesarse.");
        }
        if (this.estado != EstadoPreRegistro.PENDIENTE_VENTANILLA) {
            throw new ExcepcionDominio("El pre-registro express con código " + codigoExpress + " ya fue procesado o no está pendiente.");
        }
        this.estado = EstadoPreRegistro.PROCESADO;
    }

    public void marcarExpirado() {
        this.estado = EstadoPreRegistro.EXPIRADO;
    }

    public String getIdPreRegistro() {
        return idPreRegistro;
    }

    public String getCodigoExpress() {
        return codigoExpress;
    }

    public String getIdClienteRemitente() {
        return idClienteRemitente;
    }

    public String getIdClienteDestinatario() {
        return idClienteDestinatario;
    }

    public String getIdSucursalDestino() {
        return idSucursalDestino;
    }

    public BigDecimal getPesoEstimado() {
        return pesoEstimado;
    }

    public BigDecimal getMontoCotizado() {
        return montoCotizado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public EstadoPreRegistro getEstado() {
        return estado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreRegistroExpress that = (PreRegistroExpress) o;
        return Objects.equals(idPreRegistro, that.idPreRegistro);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPreRegistro);
    }

    @Override
    public String toString() {
        return "PreRegistroExpress{" +
                "idPreRegistro='" + idPreRegistro + '\'' +
                ", codigoExpress='" + codigoExpress + '\'' +
                ", idSucursalDestino='" + idSucursalDestino + '\'' +
                ", pesoEstimado=" + pesoEstimado +
                ", montoCotizado=" + montoCotizado +
                ", estado=" + estado +
                '}';
    }
}
