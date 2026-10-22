package pe.marvisur.sgldqr.dominio.modelo;

/**
 * Resultados posibles de una validación cruzada según el Diccionario de Datos (tabla validaciones_cruzadas_logs).
 */
public enum ResultadoValidacion {
    EXITOSO,
    FALLIDO_PAGO_PENDIENTE,
    FALLIDO_TOKEN_INVALIDO
}
