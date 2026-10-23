package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Motivos tipificados de rechazo en sincronización offline devueltos por el backend central.
 */
public enum MotivoErrorSync {
    VALIDACION,
    SUCURSAL_NO_CORRESPONDE,
    CODIGO_COMPROBANTE_DUPLICADO,
    SUCURSAL_INEXISTENTE,
    ERROR_INTERNO
}
