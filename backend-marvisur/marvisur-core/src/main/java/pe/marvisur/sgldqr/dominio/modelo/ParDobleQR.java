package pe.marvisur.sgldqr.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio (Agregado Par Doble QR) que representa el par criptográfico enlazado
 * (QR1 Operativo y QR2 Llave Digital) para una encomienda en el sistema SGL-DQR.
 *
 * <p><strong>Seguridad y Privacidad:</strong>
 * Solo se almacenan y persisten HASHES criptográficos (SHA-256). El valor en claro del token QR2
 * se entrega una sola vez al generarse y nunca se persiste ni se imprime en logs (está protegido en {@link #toString()}).
 * </p>
 */
public class ParDobleQR {

    private final String idParejaQr;
    private final String idEncomienda;
    private final String uuidQr1Operativo;
    private final String hashQr2LlaveDigital;
    private final LocalDateTime fechaGeneracion;

    /**
     * Token QR2 en texto plano. Solo tiene valor en memoria al momento de la generación
     * para entregarlo una única vez al cliente o a la capa que emitirá el QR digital.
     * Nunca se persiste en base de datos.
     */
    private final String qr2TokenEnClaro;

    private TokenQR2Estatus tokenEstatus;

    public ParDobleQR(String idParejaQr, String idEncomienda, String uuidQr1Operativo,
                      String hashQr2LlaveDigital, LocalDateTime fechaGeneracion,
                      String qr2TokenEnClaro, TokenQR2Estatus tokenEstatus) {
        this.idParejaQr = Objects.requireNonNull(idParejaQr, "El idParejaQr no puede ser nulo");
        this.idEncomienda = Objects.requireNonNull(idEncomienda, "El idEncomienda no puede ser nulo");
        this.uuidQr1Operativo = Objects.requireNonNull(uuidQr1Operativo, "El uuidQr1Operativo no puede ser nulo");
        this.hashQr2LlaveDigital = Objects.requireNonNull(hashQr2LlaveDigital, "El hashQr2LlaveDigital no puede ser nulo");
        this.fechaGeneracion = Objects.requireNonNull(fechaGeneracion, "La fechaGeneracion no puede ser nula");
        this.qr2TokenEnClaro = qr2TokenEnClaro;
        this.tokenEstatus = tokenEstatus;
    }

    public ParDobleQR(String idParejaQr, String idEncomienda, String uuidQr1Operativo,
                      String hashQr2LlaveDigital, LocalDateTime fechaGeneracion,
                      TokenQR2Estatus tokenEstatus) {
        this(idParejaQr, idEncomienda, uuidQr1Operativo, hashQr2LlaveDigital, fechaGeneracion, null, tokenEstatus);
    }

    public static ParDobleQR crearNuevo(String idEncomienda, String uuidQr1Operativo,
                                        String hashQr2LlaveDigital, LocalDateTime fechaGeneracion) {
        String idPareja = UUID.randomUUID().toString();
        TokenQR2Estatus estatus = TokenQR2Estatus.crearActivo(idPareja);
        return new ParDobleQR(idPareja, idEncomienda, uuidQr1Operativo, hashQr2LlaveDigital, fechaGeneracion, null, estatus);
    }

    public static ParDobleQR crearGenerado(String idEncomienda, String uuidQr1Operativo,
                                          String hashQr2LlaveDigital, LocalDateTime fechaGeneracion,
                                          String qr2TokenEnClaro) {
        String idPareja = UUID.randomUUID().toString();
        TokenQR2Estatus estatus = TokenQR2Estatus.crearActivo(idPareja);
        return new ParDobleQR(idPareja, idEncomienda, uuidQr1Operativo, hashQr2LlaveDigital, fechaGeneracion, qr2TokenEnClaro, estatus);
    }

    /**
     * Invalida el token QR2 de forma irreversible.
     *
     * @param motivo motivo de la invalidación
     * @param fecha  marca de tiempo
     */
    public void invalidarToken(MotivoInactivacion motivo, LocalDateTime fecha) {
        if (this.tokenEstatus != null) {
            this.tokenEstatus.invalidar(motivo, fecha);
        }
    }

    public boolean isTokenActivo() {
        return this.tokenEstatus != null && this.tokenEstatus.isActivo();
    }

    public String getIdParejaQr() {
        return idParejaQr;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public String getUuidQr1Operativo() {
        return uuidQr1Operativo;
    }

    public String getHashQr2LlaveDigital() {
        return hashQr2LlaveDigital;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public String getQr2TokenEnClaro() {
        return qr2TokenEnClaro;
    }

    public TokenQR2Estatus getTokenEstatus() {
        return tokenEstatus;
    }

    public void setTokenEstatus(TokenQR2Estatus tokenEstatus) {
        this.tokenEstatus = tokenEstatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParDobleQR that = (ParDobleQR) o;
        return Objects.equals(idParejaQr, that.idParejaQr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idParejaQr);
    }

    @Override
    public String toString() {
        return "ParDobleQR{" +
                "idParejaQr='" + idParejaQr + '\'' +
                ", idEncomienda='" + idEncomienda + '\'' +
                ", uuidQr1Operativo='" + uuidQr1Operativo + '\'' +
                ", hashQr2LlaveDigital='" + hashQr2LlaveDigital + '\'' +
                ", fechaGeneracion=" + fechaGeneracion +
                ", qr2TokenEnClaro='[PROTEGIDO]'" +
                ", tokenEstatus=" + tokenEstatus +
                '}';
    }
}
