package pe.marvisur.sgldqr.dominio.modelo;

/**
 * Estados del pago de una encomienda según el Diccionario de Datos.
 * <p>
 * Valores exactos de la columna {@code estado_pago}:
 * <ul>
 *   <li>{@link #PAGADO}: El pago fue completado al momento del despacho o recepción.</li>
 *   <li>{@link #PENDIENTE}: Pago contra entrega (bloquea la entrega física hasta su cobro).</li>
 * </ul>
 * </p>
 */
public enum EstadoPago {
    PAGADO,
    PENDIENTE
}
