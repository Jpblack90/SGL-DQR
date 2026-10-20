package pe.marvisur.core.dominio.excepcion;

/**
 * Se lanza cuando se intenta invalidar un QR2 que ya fue invalidado previamente.
 *
 * <p>La invalidación del QR2 es una operación irreversible (One-Way Seal):
 * una vez ejecutada, no puede deshacerse ni repetirse.</p>
 */
public class ExcepcionQR2YaInvalidado extends ExcepcionDominio {

    /**
     * @param mensaje descripción del intento de doble invalidación
     */
    public ExcepcionQR2YaInvalidado(String mensaje) {
        super(mensaje);
    }
}
