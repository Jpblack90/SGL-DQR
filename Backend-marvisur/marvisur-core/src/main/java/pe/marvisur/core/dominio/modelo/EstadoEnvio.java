package pe.marvisur.core.dominio.modelo;

/**
 * Ciclo de vida de una encomienda dentro del sistema SGL-DQR.
 * <p>
 * Transiciones válidas:
 * <pre>
 * REGISTRADA → EN_TRANSITO → EN_ALMACEN_DESTINO → ENTREGADA
 *                                               ↘ BLOQUEADA (si pago PENDIENTE)
 * Cualquier estado → CANCELADA (salvo ENTREGADA)
 * </pre>
 * </p>
 */
public enum EstadoEnvio {

    /** Encomienda registrada en agencia de origen, aún no despachada. */
    REGISTRADA,

    /** Encomienda en camino hacia la agencia de destino. */
    EN_TRANSITO,

    /** Encomienda llegó a la agencia de destino y espera entrega. */
    EN_ALMACEN_DESTINO,

    /**
     * Entrega bloqueada porque el estado de pago es {@link EstadoPago#PENDIENTE}.
     * Se desbloquea automáticamente al registrar el pago.
     */
    BLOQUEADA,

    /** Encomienda entregada al destinatario. Estado terminal. */
    ENTREGADA,

    /** Encomienda cancelada. Estado terminal. */
    CANCELADA
}
