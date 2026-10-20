package pe.marvisur.core.dominio.servicio;

/**
 * Resultado de la validación cruzada de los códigos QR1 y QR2
 * durante el proceso de entrega de una encomienda.
 */
public enum ResultadoValidacion {

    /**
     * Los hashes de QR1 y QR2 coinciden con los almacenados,
     * el QR2 está activo y el pago no está pendiente.
     * La entrega puede proceder.
     */
    VALIDO,

    /**
     * Los hashes calculados de los QR escaneados no coinciden
     * con los hashes SHA-256 almacenados en el sistema.
     * Posible adulteración o lectura incorrecta del QR.
     */
    HASH_NO_COINCIDE,

    /**
     * El QR2 digital ya fue invalidado en una entrega anterior.
     * No se permite reutilizar el QR2.
     */
    QR2_YA_INVALIDADO,

    /**
     * El pago de la encomienda está pendiente (modalidad contra entrega).
     * La entrega queda bloqueada hasta regularizar el pago.
     */
    PAGO_PENDIENTE
}
