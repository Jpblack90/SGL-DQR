package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Estados del ciclo de vida de un registro en la cola local SQLite (O1, O2).
 */
public enum EstadoElementoCola {
    /**
     * Registro creado localmente sin conexión, listo para enviarse al servidor central.
     */
    PENDIENTE,

    /**
     * Sincronizado exitosamente con el servidor central (o confirmado como ya sincronizado).
     */
    SINCRONIZADA,

    /**
     * Error durante la sincronización (puede ser reintentable o permanente).
     */
    ERROR
}
