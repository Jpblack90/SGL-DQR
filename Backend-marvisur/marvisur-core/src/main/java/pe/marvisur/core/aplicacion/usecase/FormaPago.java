package pe.marvisur.core.aplicacion.usecase;

/**
 * Forma de pago al momento de registrar la encomienda.
 */
public enum FormaPago {

    /** Pago inmediato en efectivo o digital al momento del registro. */
    CONTADO,

    /** Pago diferido: el destinatario paga al recibir el paquete. */
    CONTRA_ENTREGA
}
