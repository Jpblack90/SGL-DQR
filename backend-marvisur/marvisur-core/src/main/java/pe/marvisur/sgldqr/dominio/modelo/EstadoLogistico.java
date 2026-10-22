package pe.marvisur.sgldqr.dominio.modelo;

/**
 * Estados del ciclo de vida logístico de una encomienda según el Diccionario de Datos.
 * <p>
 * Valores exactos de la columna {@code estado_logistico}:
 * <ul>
 *   <li>{@link #REGISTRADO}: Encomienda registrada en ventanilla de la sucursal origen.</li>
 *   <li>{@link #EN_ALMACEN_ORIGEN}: Recepcionada y clasificada en el almacén de origen.</li>
 *   <li>{@link #EN_RUTA}: Asignada a un viaje interprovincial en tránsito.</li>
 *   <li>{@link #EN_ALMACEN_DESTINO}: Arribada a la sucursal de destino y lista para entrega.</li>
 *   <li>{@link #ENTREGADO}: Entregada exitosamente al cliente destinatario.</li>
 * </ul>
 * </p>
 */
public enum EstadoLogistico {
    REGISTRADO,
    EN_ALMACEN_ORIGEN,
    EN_RUTA,
    EN_ALMACEN_DESTINO,
    ENTREGADO
}
