package pe.marvisur.core.dominio.modelo;

/**
 * Estado del QR2 digital asociado a una encomienda.
 * <p>
 * Una vez {@code INVALIDADO}, el cambio es irreversible: el QR2 no puede
 * volver a activarse, garantizando que la entrega no sea confirmada dos veces.
 * </p>
 */
public enum EstadoQR2 {

    /** El QR2 digital está vigente y puede usarse para validar la entrega. */
    ACTIVO,

    /**
     * El QR2 fue invalidado de forma irreversible tras una entrega exitosa.
     * La fecha de invalidación queda registrada en {@link CodigoQR#getFechaInvalidacion()}.
     */
    INVALIDADO
}
