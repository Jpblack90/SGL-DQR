package pe.marvisur.sgldqr.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa el par enlazado de códigos QR (QR1 Operativo y QR2 Llave Digital)
 * para una encomienda en el sistema SGL-DQR.
 */
public class ParDobleQR {

    private final String idParejaQr;
    private final String idEncomienda;
    private final String uuidQr1Operativo;
    private final String hashQr2LlaveDigital;
    private final LocalDateTime fechaGeneracion;

    public ParDobleQR(String idParejaQr, String idEncomienda, String uuidQr1Operativo,
                      String hashQr2LlaveDigital, LocalDateTime fechaGeneracion) {
        this.idParejaQr = Objects.requireNonNull(idParejaQr, "El idParejaQr no puede ser nulo");
        this.idEncomienda = Objects.requireNonNull(idEncomienda, "El idEncomienda no puede ser nulo");
        this.uuidQr1Operativo = Objects.requireNonNull(uuidQr1Operativo, "El uuidQr1Operativo no puede ser nulo");
        this.hashQr2LlaveDigital = Objects.requireNonNull(hashQr2LlaveDigital, "El hashQr2LlaveDigital no puede ser nulo");
        this.fechaGeneracion = Objects.requireNonNull(fechaGeneracion, "La fechaGeneracion no puede ser nula");
    }

    public static ParDobleQR crearNuevo(String idEncomienda, String uuidQr1Operativo,
                                        String hashQr2LlaveDigital, LocalDateTime fechaGeneracion) {
        return new ParDobleQR(UUID.randomUUID().toString(), idEncomienda, uuidQr1Operativo, hashQr2LlaveDigital, fechaGeneracion);
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
}
