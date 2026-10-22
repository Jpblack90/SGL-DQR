package pe.marvisur.sgldqr.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa el ciclo de vida y estado del token digital QR2
 * según la tabla tokens_qr2_estatus del Diccionario de Datos.
 */
public class TokenQR2Estatus {

    private final String idTokenStatus;
    private final String idParejaQr;
    private EstadoTokenQR2 estadoToken;
    private LocalDateTime fechaInvalidacion;
    private MotivoInactivacion motivoInactivacion;

    public TokenQR2Estatus(String idTokenStatus, String idParejaQr, EstadoTokenQR2 estadoToken,
                           LocalDateTime fechaInvalidacion, MotivoInactivacion motivoInactivacion) {
        this.idTokenStatus = Objects.requireNonNull(idTokenStatus, "El idTokenStatus no puede ser nulo");
        this.idParejaQr = Objects.requireNonNull(idParejaQr, "El idParejaQr no puede ser nulo");
        this.estadoToken = Objects.requireNonNull(estadoToken, "El estadoToken no puede ser nulo");
        this.fechaInvalidacion = fechaInvalidacion;
        this.motivoInactivacion = motivoInactivacion;
    }

    public static TokenQR2Estatus crearActivo(String idParejaQr) {
        return new TokenQR2Estatus(
                UUID.randomUUID().toString(),
                idParejaQr,
                EstadoTokenQR2.ACTIVO,
                null,
                null
        );
    }

    /**
     * Invalida el token QR2 de forma irreversible.
     *
     * @param motivo motivo oficial de la inactivación (ej. ENTREGA_EXITOSA o INTENTO_FRAUDE)
     * @param fecha  marca temporal del suceso
     */
    public void invalidar(MotivoInactivacion motivo, LocalDateTime fecha) {
        if (this.estadoToken == EstadoTokenQR2.INVALIDADO) {
            throw new IllegalStateException("El token QR2 ya se encuentra INVALIDADO de forma irreversible.");
        }
        this.estadoToken = EstadoTokenQR2.INVALIDADO;
        this.motivoInactivacion = Objects.requireNonNull(motivo, "El motivo no puede ser nulo");
        this.fechaInvalidacion = Objects.requireNonNull(fecha, "La fecha no puede ser nula");
    }

    public boolean isActivo() {
        return this.estadoToken == EstadoTokenQR2.ACTIVO;
    }

    public boolean isInvalidado() {
        return this.estadoToken == EstadoTokenQR2.INVALIDADO;
    }

    public String getIdTokenStatus() {
        return idTokenStatus;
    }

    public String getIdParejaQr() {
        return idParejaQr;
    }

    public EstadoTokenQR2 getEstadoToken() {
        return estadoToken;
    }

    public LocalDateTime getFechaInvalidacion() {
        return fechaInvalidacion;
    }

    public MotivoInactivacion getMotivoInactivacion() {
        return motivoInactivacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenQR2Estatus that = (TokenQR2Estatus) o;
        return Objects.equals(idTokenStatus, that.idTokenStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTokenStatus);
    }

    @Override
    public String toString() {
        return "TokenQR2Estatus{" +
                "idTokenStatus='" + idTokenStatus + '\'' +
                ", idParejaQr='" + idParejaQr + '\'' +
                ", estadoToken=" + estadoToken +
                ", fechaInvalidacion=" + fechaInvalidacion +
                ", motivoInactivacion=" + motivoInactivacion +
                '}';
    }
}
