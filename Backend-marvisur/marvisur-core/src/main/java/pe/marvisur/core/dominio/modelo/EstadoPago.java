package pe.marvisur.core.dominio.modelo;

/**
 * Estado del pago de una encomienda.
 * <p>
 * Si el estado es {@code PENDIENTE}, la entrega queda bloqueada
 * hasta que el remitente o destinatario regularice el pago (contra entrega).
 * </p>
 */
public enum EstadoPago {

    /** El pago aún no se ha realizado (modalidad contra entrega). */
    PENDIENTE,

    /** El pago fue recibido correctamente. */
    PAGADO,

    /** La operación fue cancelada y el pago revertido o no cobrado. */
    CANCELADO
}
