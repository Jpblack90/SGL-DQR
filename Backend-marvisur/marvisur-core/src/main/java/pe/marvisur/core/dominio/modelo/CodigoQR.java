package pe.marvisur.core.dominio.modelo;

import pe.marvisur.core.dominio.excepcion.ExcepcionQR2YaInvalidado;

import java.time.LocalDateTime;

/**
 * Entidad que agrupa el par de códigos QR (físico y digital) asociados
 * a una {@link Encomienda} dentro del sistema de Doble QR (SGL-DQR).
 *
 * <p><strong>Reglas de seguridad:</strong></p>
 * <ul>
 *   <li>Nunca se almacenan los valores originales de los QR, solo sus hashes SHA-256.</li>
 *   <li>El QR2 ({@code qr2Hash}) solo puede invalidarse una vez de forma irreversible.</li>
 *   <li>La invalidación del QR2 registra la fecha y hora exacta del evento.</li>
 * </ul>
 */
public class CodigoQR {

    // -------------------------------------------------------------------------
    // Atributos
    // -------------------------------------------------------------------------

    /** Identificador de la encomienda a la que pertenece este par de QR (1..1). */
    private final String encomiendaId;

    /** Hash SHA-256 del QR1 físico impreso en el ticket de papel. */
    private String qr1Hash;

    /** Hash SHA-256 del QR2 digital enviado al destinatario. */
    private String qr2Hash;

    /** Estado actual del QR2 digital. */
    private EstadoQR2 qr2Estado;

    /**
     * Fecha y hora en que se invalidó el QR2.
     * Es {@code null} mientras el QR2 esté en estado {@link EstadoQR2#ACTIVO}.
     */
    private LocalDateTime fechaInvalidacion;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Crea un par de códigos QR para la encomienda indicada.
     * El QR2 inicia siempre en estado {@link EstadoQR2#ACTIVO}.
     *
     * @param encomiendaId identificador UUID de la encomienda
     * @param qr1Hash      hash SHA-256 del QR1 físico
     * @param qr2Hash      hash SHA-256 del QR2 digital
     */
    public CodigoQR(String encomiendaId, String qr1Hash, String qr2Hash) {
        this.encomiendaId = encomiendaId;
        this.qr1Hash = qr1Hash;
        this.qr2Hash = qr2Hash;
        this.qr2Estado = EstadoQR2.ACTIVO;
        this.fechaInvalidacion = null;
    }

    /**
     * Constructor completo para reconstruir desde persistencia.
     *
     * @param encomiendaId     identificador de la encomienda
     * @param qr1Hash          hash SHA-256 del QR1
     * @param qr2Hash          hash SHA-256 del QR2
     * @param qr2Estado        estado del QR2
     * @param fechaInvalidacion fecha de invalidación o {@code null} si activo
     */
    public CodigoQR(String encomiendaId, String qr1Hash, String qr2Hash,
                    EstadoQR2 qr2Estado, LocalDateTime fechaInvalidacion) {
        this.encomiendaId = encomiendaId;
        this.qr1Hash = qr1Hash;
        this.qr2Hash = qr2Hash;
        this.qr2Estado = qr2Estado;
        this.fechaInvalidacion = fechaInvalidacion;
    }

    // -------------------------------------------------------------------------
    // Métodos de dominio
    // -------------------------------------------------------------------------

    /**
     * Invalida el QR2 digital de forma irreversible.
     * <p>
     * Cambia {@code qr2Estado} a {@link EstadoQR2#INVALIDADO} y registra
     * la fecha/hora actual como {@code fechaInvalidacion}.
     * </p>
     *
     * @return {@code true} si la invalidación fue exitosa
     * @throws ExcepcionQR2YaInvalidado si el QR2 ya estaba invalidado
     */
    public boolean invalidarQR2() {
        if (this.qr2Estado == EstadoQR2.INVALIDADO) {
            throw new ExcepcionQR2YaInvalidado(
                    "El QR2 de la encomienda '" + encomiendaId
                            + "' ya fue invalidado el " + fechaInvalidacion
                            + ". La operación es irreversible.");
        }
        this.qr2Estado = EstadoQR2.INVALIDADO;
        this.fechaInvalidacion = LocalDateTime.now();
        return true;
    }

    // -------------------------------------------------------------------------
    // Getters y Setters
    // -------------------------------------------------------------------------

    /** @return identificador de la encomienda asociada */
    public String getEncomiendaId() {
        return encomiendaId;
    }

    /** @return hash SHA-256 del QR1 físico */
    public String getQr1Hash() {
        return qr1Hash;
    }

    /** @param qr1Hash nuevo hash del QR1 */
    public void setQr1Hash(String qr1Hash) {
        this.qr1Hash = qr1Hash;
    }

    /** @return hash SHA-256 del QR2 digital */
    public String getQr2Hash() {
        return qr2Hash;
    }

    /** @param qr2Hash nuevo hash del QR2 */
    public void setQr2Hash(String qr2Hash) {
        this.qr2Hash = qr2Hash;
    }

    /** @return estado actual del QR2 */
    public EstadoQR2 getQr2Estado() {
        return qr2Estado;
    }

    /** @param qr2Estado nuevo estado del QR2 */
    public void setQr2Estado(EstadoQR2 qr2Estado) {
        this.qr2Estado = qr2Estado;
    }

    /** @return fecha y hora de invalidación, o {@code null} si el QR2 está activo */
    public LocalDateTime getFechaInvalidacion() {
        return fechaInvalidacion;
    }

    /** @param fechaInvalidacion fecha de invalidación a registrar */
    public void setFechaInvalidacion(LocalDateTime fechaInvalidacion) {
        this.fechaInvalidacion = fechaInvalidacion;
    }

    @Override
    public String toString() {
        return "CodigoQR{encomiendaId='" + encomiendaId
                + "', qr2Estado=" + qr2Estado
                + ", fechaInvalidacion=" + fechaInvalidacion + '}';
    }
}
