package pe.marvisur.core.dominio.excepcion;

/**
 * Se lanza cuando se intenta entregar una encomienda cuyo pago está pendiente.
 *
 * <p>Regla de negocio: si {@code estadoPago == PENDIENTE}, la entrega queda
 * bloqueada hasta que el pago sea registrado por un {@link pe.marvisur.core.dominio.modelo.Rol#TN3_ADMINISTRADOR}
 * o el sistema reciba confirmación de pago contra entrega.</p>
 */
public class ExcepcionEntregaBloqueada extends ExcepcionDominio {

    /**
     * @param mensaje descripción del bloqueo de entrega por pago pendiente
     */
    public ExcepcionEntregaBloqueada(String mensaje) {
        super(mensaje);
    }
}
