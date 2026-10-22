package pe.marvisur.sgldqr.api.dto;

/**
 * Motivos tipificados de error al procesar un ítem en un lote de escaneo según los requisitos de RF-06.
 */
public enum MotivoErrorLote {
    QR1_INEXISTENTE,
    ESTADO_INVALIDO,
    SUCURSAL_NO_CORRESPONDE,
    DESTINO_NO_COINCIDE,
    YA_DESPACHADA,
    VIAJE_INEXISTENTE
}
